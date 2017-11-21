'''
Created on 3 mei 2017

@author: sibeleker
@author: jhkwakkel
'''
from __future__ import (unicode_literals, absolute_import, division,
                        print_function)
import itertools
import numpy as np
import os
import pandas as pd
import time

from  multiprocessing import Process

from ema_workbench.em_framework import (RealParameter, CategoricalParameter, 
                                    ScalarOutcome, MultiprocessingEvaluator)
from ema_workbench.util import ema_logging, save_results
from ema_workbench.util.ema_logging import method_logger

from simzmq import SimZMQModel
from federatestarter import FederateStarter
from ema_workbench.em_framework.evaluators import perform_experiments
from ema_workbench.em_framework.parameters import Policy

goods = ['Textile', 'Garment', 'Steel', 'Brick', 'Food']
    

def beta_growth_function(t, w_max=1, t_e=5, t_m=0.5):
    s1 = 1 + (t_e - t) / (t_e - t_m)
    exponent = t_e / (t_e - t_m)
    s2 = np.power((t / t_e), exponent)
    y = w_max * s1 * s2
    return y


class BGD_TransportModel(SimZMQModel):
    infrastructure = ['road', 'bridge', 'waterway', 'ferry', 'ports', 'terminals', 
                      'railways', 'railstations']
    categories = {'road' : ['n', 'r', 'z'],
                  'bridge' : ['a', 'b', 'c', 'd'],
                  'waterway' : ['1', '2', '3', '4']}

    @property
    def run_setup(self):
        return [self._runtime, self._warmuptime, self._offsettime, self._speed]

    @run_setup.setter
    def run_setup(self, params):
        self._runtime = params[0]
        self._warmuptime = params[1]
        self._offsettime = params[2]
        self._speed = params[3]
    
    @method_logger
    def run_experiment(self, experiment):
        self.start_new_model()
        run_id = experiment.id
        
        #         #1) === SETTING THE PARAMETER VALUES ONE BY ONE ===
        water_depth = experiment.pop('Flood_depth')
        damage_ratios = {}
        for key in self.infrastructure:
            if key in ['road', 'bridge', 'waterway']:
                for category in self.categories[key]:
                    wm = experiment.pop('{}_{}_Wmax'.format(key, category))
                    tm = experiment.pop('{}_{}_Tm'.format(key, category))
                    damage = beta_growth_function(water_depth, w_max=wm, t_m=tm)
                    damage_ratios['Damage_'+key+'_'+category] = damage
            else:
                wm = experiment.pop(key+'_Wmax')
                tm = experiment.pop(key+'_Tm')
                damage = beta_growth_function(water_depth, w_max=wm, t_m=tm)
                damage_ratios['Damage_'+key] = damage
         
        for key, value in experiment.items():
            # send the parameters that are not included above
            payload = [key, value]
            self.set_value(run_id, payload) 
             
        for key, value in damage_ratios.items():
            payload = [key, value]
            self.set_value(run_id, payload)
        
        #2) === RUN THE SIMULATION ===
        self.start_simulation(run_id)
            
        # 3) === REQUEST STATUS ===
        wait = True
        while wait:
            wait = self.RequestStatus(run_id)
            time.sleep(2)
        
        #4) === COLLECT THE SIMULATION RESULTS ===
        results = damage_ratios.copy()
        for outcome in self.outcomes:
            if outcome.name.startswith('Damage_'):
                continue 
            
            v_type = type(outcome).__name__.split("O")[0]
            for var in outcome.variable_name:
                results[var] = self.RequestStatistics(run_id, var, v_type)
        
        ema_logging.debug('setting results to output')
        
        return results
    

def uncertainty_factory():
    
# [GARMENT_IMPORT, GARMENT_PRODUCTION, FOOD_CONSUMPTION, STEEL_CONSUMPTION, 
#  TEXTILE_PRODUCTION, TEXTILE_CONSUMPTION, BRICK_EXPORT, FOOD_IMPORT, 
#  STEEL_IMPORT, GARMENT_CONSUMPTION, STEEL_PRODUCTION, BRICK_PRODUCTION, 
#  STEEL_EXPORT, TEXTILE_EXPORT, BRICK_IMPORT, GARMENT_EXPORT, TEXTILE_IMPORT, 
#  BRICK_CONSUMPTION, FOOD_PRODUCTION, FOOD_EXPORT] 
    activity = ['production', 'consumption', 'export', 'import']
    modes = ['road', 'rail', 'water']
    fnc_parameters = ['Wmax', 'Tm']
    flood_ids = pd.read_excel('./data/Scenario_data.xlsx', 
                             sheetname='FloodArea_2')['Flood_ID']
    flood_ids = flood_ids[::10]
    boundaries = pd.read_excel('./data/Scenario_data.xlsx', 
                               sheetname='Damage_parameters')
    boundaries = boundaries.drop('Infrastructure', axis=1)
    boundaries = boundaries.set_index('Parameter')
    
#     socioeconomic_parameters = [pair[0]+'_'+pair[1] for pair in 
#                                 itertools.product(goods, activity)]
#     socioeconomic_unc = []
#     socioeconomic_unc = [RealParameter(prm, 0, 2) for prm in 
#                          socioeconomic_parameters]

    transport_parameters = [pair[0]+'_'+pair[1] for pair in 
                            itertools.product(goods, modes)]
    transport_unc = [RealParameter(prm, 0, 1) for prm in transport_parameters] 
    
    damage_parameters = {}
    names = []
    for infra in model.infrastructure:
        if infra in ['road', 'bridge', 'waterway']:
            dmg_parameters = ['{}_{}_{}'.format(infra, *pair) for pair in 
                              itertools.product(model.categories[infra], 
                                                fnc_parameters)]   
        else:
            dmg_parameters = [infra+'_'+prm for prm in fnc_parameters]
        damage_parameters[infra] = dmg_parameters
        names.extend(dmg_parameters)
    
    damage_uncertainties = []
    for infra in model.infrastructure:
        parameters = damage_parameters[infra]
        for parameter in parameters:
            lower = boundaries.loc[parameter, 'Lower'] 
            upper = boundaries.loc[parameter, 'Upper']
            damage_uncertainties.append(RealParameter(parameter, lower, upper))
            
    other_unc = [CategoricalParameter('Flood_area', flood_ids, pff=True),
                 RealParameter('Flood_duration', 30, 90),
                 RealParameter('Flood_depth', 1, 5),
                 RealParameter('Water_cost', 1, 5),
                 RealParameter('Road_cost', 3,9),
                 RealParameter('Trs_cost', 200, 500)]
    return transport_unc + damage_uncertainties + other_unc

def outcome_factory():
    # define outcomes
    outcomes = [ScalarOutcome("{}_TransportCost".format(good)) for good 
                      in goods]
    outcomes.extend([ScalarOutcome("{}_TravelTime".format(good),) for good 
                           in goods])
    outcomes.extend([ScalarOutcome("{}_UnsatisfiedDemand".format(good)) for 
                     good in goods])
    outcomes.extend([ScalarOutcome(name) for name in ['Damage_road_n', 
                    'Damage_road_r', 'Damage_road_z', 
                    'Damage_bridge_a', 'Damage_bridge_b', 'Damage_bridge_c', 
                    'Damage_bridge_d', 'Damage_waterway_1', 'Damage_waterway_2', 
                    'Damage_waterway_3', 'Damage_waterway_4', 'Damage_ferry', 
                    'Damage_ports', 'Damage_terminals', 'Damage_railways', 
                    'Damage_railstations']])
    return outcomes

if __name__ == "__main__":       
    ema_logging.log_to_stderr(ema_logging.INFO)
    
    ip = 'localhost'
    federatestarter_port = '5555'
    federatestarter_name = "FS"
    magic_nr = "SIM01"
    directory = os.path.abspath('./model')
    wd = os.path.abspath('./wd')
    secsperday = 60*60*24.0
    run_days = 60
    startup_days = 10

    fs = Process(target=FederateStarter, args=('federation_name', 
                                          magic_nr, 
                                          federatestarter_port, 
                                          federatestarter_name, 
                                          5000, 6000))
    fs.deamon = True
    fs.start()
  
    model = BGD_TransportModel(name="BGD", 
                        wd=wd,
                        software_code='java',
                        args_before='-jar', 
                        args_after=directory, # TODO:: directory where the jar resides
                        fullPathModelFile='./model/bgd.jar', 
                        redirectStdin='', 
                        redirectStdout="out.txt",
                        redirectStderr="err.txt",
                        ip=ip, 
                        federatestarter_port=federatestarter_port, 
                        federatestarter_name=federatestarter_name, 
                        receiver_tag="BGD", 
                        magic_nr=magic_nr, 
                        sim_run_id="FM", 
                        sender_id="EMA",)
    model.run_setup = [run_days*secsperday, startup_days*secsperday, 0.0, 
                       1000000000.0]
    model.n_replications = 1
    model.uncertainties = uncertainty_factory()
    model.outcomes = outcome_factory()
    model.levers = [CategoricalParameter('Intervention_{}'.format(i), [False, True]) for i in range(1,13)]
 
    n_floods = len(model.uncertainties['Flood_area'].categories)
 
    n_experiments = 1000
    with MultiprocessingEvaluator(model, n_processes=40) as evaluator:
        policies = [Policy('Intervention_{}'.format(i), **{'Intervention_{}'.format(i):True}) for i in range(1,13)]
        policies.append(Policy('no intervention', **{}))
        results = evaluator.perform_experiments(n_experiments,
                                                policies,
                                                uncertainty_sampling='pff',
                                                reporting_interval=1)
    save_results(results, './results/pff {} floods {} cases with interventions.tar.gz'.format(n_floods, n_experiments))
     
#     results = perform_experiments(model, 2, reporting_interval=1, 
#                                   uncertainty_sampling='pff')
    save_results(results, 'reference.tar.gz')
    print(results[1])
    fs.terminate()
