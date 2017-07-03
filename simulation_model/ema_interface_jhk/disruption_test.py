'''
Created on 3 mei 2017

@author: sibeleker
'''
import itertools
import numpy as np
import os
import pandas as pd
import time

from  multiprocessing import Process

from ema_workbench.em_framework.outcomes import ScalarOutcome
from ema_workbench.em_framework import (RealParameter, CategoricalParameter, 
                                        perform_experiments)
import ema_workbench.em_framework.util as util
from ema_workbench.util import ema_logging
from ema_workbench.util.ema_logging import method_logger

from zero_mq_v4 import ZeroMQModel
from FederateStarter_router import FederateStarter
from numpy.core.defchararray import upper


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


class BGD_TransportModel(ZeroMQModel):
    def __init__(self, name, softwareCode, argsBefore, fullPathModelFile, 
                 argsAfter, fs_workingDirectory, redirectStdin, redirectStdout, 
                 redirectStderr, deleteWorkingDirectory, deleteStdin, 
                 deleteStdout, deleteStderr, ip, fs_port, fs_receiver, 
                 m_receiver, magic_no, sim_run_id, sender_id, local_directory):
        
        super(BGD_TransportModel, self).__init__(name, softwareCode,
                 argsBefore, fullPathModelFile, argsAfter,
                 fs_workingDirectory, redirectStdin, redirectStdout, redirectStderr,
                 deleteWorkingDirectory, deleteStdin, deleteStdout, deleteStderr,
                 ip, fs_port, fs_receiver, m_receiver, 
                 magic_no, sim_run_id, sender_id, local_directory)
    
    @method_logger
    def run_experiment(self, experiment):
        super(BGD_TransportModel, self).run_experiment(experiment)
        run_id = experiment.name + 1 #to avoid 0
        
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
                    #del experiment[category+'_Wmax']
                    #del experiment[category+'_Tm']
                    experiment.pop(category+'_Wmax', None)
                    experiment.pop(category+'_Tm', None)
            else:
                wm = experiment[key+'_Wmax']
                tm = experiment[key+'_Tm']
                damage = beta_growth_function(water_depth, w_max=wm, t_m=tm)
                damage_ratios['Damage_'+key] = damage
                #del experiment[key+'_Wmax']
                #del experiment[key+'_Tm']
                experiment.pop(key+'_Wmax', None)
                experiment.pop(key+'_Tm', None)
        #del experiment['Flood_depth']
        experiment.pop('Flood_depth', None)
        
        for key, value in experiment.items():
            # send the parameters that are not included above
            payload = [key, value]
            self.SetValue(run_id, payload) 
            
        for key, value in damage_ratios.items():
            payload = [key, value]
            self.SetValue(run_id, payload)
        
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
            variable = outcome.variable_name[0]
            v_type = type(outcome).__name__.split("O")[0]
            
            results[variable] = self.RequestStatistics(run_id, variable, v_type)
        
        ema_logging.debug('setting results to output')
        self.output = results
        

if __name__ == "__main__":       
     
    ip = 'localhost'
    fs_port = '5555'
    Process(target=FederateStarter, args=('federation_name', "SIM01", 
                                          fs_port, 'FS', 5000, 6000)).start()

    ema_logging.log_to_stderr(ema_logging.INFO)
    
    directory = os.path.abspath('./model')
    model = BGD_TransportModel(name="mm1q", 
                        softwareCode='java',
                        argsBefore='-jar', 
                        fullPathModelFile='./model/bgd.jar', 
                        argsAfter='BGD.1 5556 {}'.format(directory), 
                        fs_workingDirectory=directory, 
                        redirectStdin='', 
                        redirectStdout= os.path.join(directory, "out.txt"),
                        redirectStderr= os.path.join(directory, "err.txt"),
                        deleteWorkingDirectory=False, deleteStdin=False, 
                        deleteStdout=False, deleteStderr=False, ip=ip, 
                        fs_port=fs_port, 
                        fs_receiver="FS", 
                        m_receiver="BGD.", 
                        magic_no="SIM01", 
                        sim_run_id="FM", 
                        sender_id="EMA",
                        local_directory=directory)
       

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
    outcomes.extend([ScalarOutcome("{}_TravelTime".format(good)) for good 
                           in goods])
    outcomes.extend([ScalarOutcome("{}_UnsatisfiedDemand".format(good)) for 
                     good in goods])
    
    # set uncertainties and outcomes on model
    model.uncertainties = socioeconomic_unc + transport_unc \
                    + damage_uncertainties + other_unc
    model.outcomes = outcomes
    
    print([out.name for out in model.outcomes])
    print([unc.name for unc in model.uncertainties])
 
    
    results = perform_experiments(model, 2)
    experiments, outcomes = results
    print(outcomes)
