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
from ema_workbench.util import ema_logging
from ema_workbench.util.ema_logging import method_logger

from simzmq import SimZMQModel
from federatestarter import FederateStarter


infrastructure = ['road', 'bridge', 'waterway', 'ferry', 'ports', 'terminals', 
                  'railways', 'railstations']
roads = ['n', 'r', 'z']
bridges = ['a', 'b', 'c', 'd']
waterways = ['1', '2', '3', '4']
categories = {'road' : roads,
              'bridge' : bridges,
              'waterway' : waterways}

goods = ['Textile', 'Garment', 'Steel', 'Brick', 'Food']
activity = ['production', 'consumption', 'export', 'import']
modes = ['road', 'rail', 'waterway']
fnc_parameters = ['Wmax', 'Tm']

flood_ids = pd.read_excel('./data/Scenario_data.xlsx', 
                         sheetname='FloodArea_2')['Flood_ID']

boundaries = pd.read_excel('./data/Scenario_data.xlsx', 
                           sheetname='Damage_parameters')
boundaries = boundaries.drop('Infrastructure', axis=1)
boundaries = boundaries.set_index('Parameter')


def beta_growth_function(t, w_max=1, t_e=5, t_m=0.5):
    s1 = 1 + (t_e - t) / (t_e - t_m)
    exponent = t_e / (t_e - t_m)
    s2 = np.power((t / t_e), exponent)
    y = w_max * s1 * s2
    return y


class BGD_TransportModel(SimZMQModel):
    
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
        run_id = experiment.id + 1 #to avoid 0
        
        #1) === SETTING THE PARAMETER VALUES ONE BY ONE ===
        water_depth = experiment['Flood_depth']
        damage_ratios = {}
        for key in infrastructure:
            if key in ['road', 'bridge', 'waterway']:
                for category in categories[key]:
                    wm = experiment[category+'_Wmax']
                    tm = experiment[category+'_Tm']
                    damage = beta_growth_function(water_depth, w_max=wm, t_m=tm)
                    damage_ratios['Damage_'+key+'_'+category] = damage
                    experiment.pop(category+'_Wmax', None)
                    experiment.pop(category+'_Tm', None)
            else:
                wm = experiment[key+'_Wmax']
                tm = experiment[key+'_Tm']
                damage = beta_growth_function(water_depth, w_max=wm, t_m=tm)
                damage_ratios['Damage_'+key] = damage
                experiment.pop(key+'_Wmax', None)
                experiment.pop(key+'_Tm', None)
        experiment.pop('Flood_depth', None)
        
        for key, value in experiment.items():
            # send the parameters that are not included above
            payload = [key, value]
            self.set_value(run_id, payload) 
            
        for key, value in damage_ratios.items():
            payload = [key, value]
            self.set_value(run_id, payload)
        
        #2) === RUN THE SIMULATION ===
        self.StartSimulation(run_id)
            
        # 3) === REQUEST STATUS ===
        wait = True
        while wait:
            wait = self.RequestStatus(run_id)
            time.sleep(2)
        
        #4) === COLLECT THE SIMULATION RESULTS ===
        results = {}
        for outcome in self.outcomes:
            v_type = type(outcome).__name__.split("O")[0]
            for var in outcome.variable_name:
                results[var] = self.RequestStatistics(run_id, var, v_type)
        
        ema_logging.debug('setting results to output')
        return results
        

if __name__ == "__main__":       
    ema_logging.log_to_stderr(ema_logging.INFO)
    
    ip = 'localhost'
    federatestarter_port = '5555'
    federatestarter_name = "FS"
    magic_nr = "SIM01"
    Process(target=FederateStarter, args=('federation_name', 
                                          magic_nr, 
                                          federatestarter_port, 
                                          federatestarter_name, 
                                          5000, 6000)).start()

    # TODO::
    directory = os.path.abspath('./model')
    wd = os.path.abspath('./wd')
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
                        m_receiver="BGD", 
                        magic_nr=magic_nr, 
                        sim_run_id="FM", 
                        sender_id="EMA",)
    model.run_setup = [60*24.0, 0.0, 0.0, 1000000000.0]
    
    #TODO FM.2 message klopt nog niet
    # replications moeten correct er in komen
    model.n_replications = 2

    socioeconomic_parameters = [pair[0]+'_'+pair[1] for pair in 
                                itertools.product(goods, activity)]
    socioeconomic_unc = [RealParameter(prm, 0, 2) for prm in 
                         socioeconomic_parameters]
    
    transport_parameters = list(itertools.product(goods, modes))
    transport_parameters = [pair[0]+'_'+pair[1] for pair in transport_parameters]
    #make sure they add up to 1
    transport_unc = [RealParameter(prm, 0, 1) for prm in transport_parameters] 
    
    damage_parameters = {}
    for infra in infrastructure:
        if infra in ['road', 'bridge', 'waterway']:
            dmg_parameters = [pair[0]+'_'+pair[1] for pair in 
                              itertools.product(categories[infra], 
                                                fnc_parameters)]   
        else:
            dmg_parameters = [infra+'_'+prm for prm in fnc_parameters]
        damage_parameters[infra] = dmg_parameters

    damage_uncertainties = []
    for infra in infrastructure:
        parameters = damage_parameters[infra]
        for parameter in parameters:
            lower = boundaries.loc[parameter, 'Lower'] 
            upper = boundaries.loc[parameter, 'Upper']
            damage_uncertainties.append(RealParameter(parameter, lower, upper))
            
    other_unc = [CategoricalParameter('Flood_area', flood_ids),
                 RealParameter('Flood_duration', 1, 90),
                 RealParameter('Flood_depth', 0, 5)]
                        
    # define outcomes
    outcomes = [ScalarOutcome("{}_TransportCost".format(good)) for good 
                      in goods]
    outcomes.extend([ScalarOutcome("{}_TravelTime".format(good),) for good 
                           in goods])
    outcomes.extend([ScalarOutcome("{}_UnsatisfiedDemand".format(good)) for 
                     good in goods])
    
    # set uncertainties and outcomes on model
    model.uncertainties = socioeconomic_unc + transport_unc \
                    + damage_uncertainties + other_unc
    model.outcomes = outcomes
 
    with MultiprocessingEvaluator(model) as evaluator:
        results = evaluator.perform_experiments(3, reporting_interval=1)    
    experiments, outcomes = results
    print(outcomes)
