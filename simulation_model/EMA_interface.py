'''
Created on 3 mei 2017

@author: sibeleker
'''
from zero_mq_v4 import ZeroMQModel
from FederateStarter_router import FederateStarter

from ema_workbench.em_framework.outcomes import ScalarOutcome
from ema_workbench.em_framework import (Policy, RealParameter, 
                                    CategoricalParameter, perform_experiments)

from ema_workbench.util import save_results
from ema_workbench.util import ema_logging

import ema_workbench.em_framework.util as util


from  multiprocessing import Process

import time
import numpy as np
import itertools
import pandas as pd

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


def beta_growth_function(t, w_max=1, t_e=5, t_m=0.5):
    s1 = 1 + (t_e - t) / (t_e - t_m)
    exponent = t_e / (t_e - t_m)
    s2 = np.power((t / t_e), exponent)
    y = w_max * s1 * s2
    return y


class BGD_TransportModel(ZeroMQModel):
    def __init__(self, name, softwareCode,
                 argsBefore, fullPathModelFile, argsAfter,
                 fs_workingDirectory, redirectStdin, redirectStdout, redirectStderr,
                 deleteWorkingDirectory, deleteStdin, deleteStdout, deleteStderr,
                 ip, fs_port, fs_receiver, m_receiver, 
                 magic_no, sim_run_id, sender_id, local_directory):
        
        super(BGD_TransportModel, self).__init__(name, softwareCode,
                 argsBefore, fullPathModelFile, argsAfter,
                 fs_workingDirectory, redirectStdin, redirectStdout, redirectStderr,
                 deleteWorkingDirectory, deleteStdin, deleteStdout, deleteStderr,
                 ip, fs_port, fs_receiver, m_receiver, 
                 magic_no, sim_run_id, sender_id, local_directory)
    
    def run_model(self, scenario, policy, experiment_id, instance_id):
        
        super(BGD_TransportModel, self).run_model(scenario, policy, experiment_id, instance_id)
        experiment = util.combine(scenario, policy)
        run_id = experiment_id + 1 #to avoid 0
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
    fnc_parameters = ['Wmax', 'Tm']
    
    flood_df = pd.read_excel('./data/Scenario_data.xlsx',
                             sheetname='FloodArea')
    flood_ids = flood_df['Flood_id']
    del flood_df
    
    damage_boundaries = pd.read_excel('./data/Scenario_data.xlsx',
                                      sheetname='Damage_parameters')
    damage_boundaries = damage_boundaries.drop('Infrastructure', axis=1)
    damage_boundaries = damage_boundaries.set_index('Parameter')
    
#     boundaries = pd.read_excel('./Model_Inputs/parameter_list.xlsx')
#     boundaries = boundaries.set_index('Parameter')
     
    ip = 'localhost'
    fs_port = '5555'
    Process(target=FederateStarter, args=('federation_name', "SIM01", fs_port, 'FS', 5000, 6000)).start()

    ema_logging.log_to_stderr(ema_logging.INFO)
    
    local_directory = 'D:/sibeleker/workspace/BangladeshTransport/static_scenario_analysis'
    cluster_directory = 'H:/BGD_static'
    cluster_wb_directory = 'C:/Users/sibeleker/Documents/BGD/'
    directory = cluster_directory
    model = BGD_TransportModel(name="BGD", 
                        softwareCode='python',
                        argsBefore='', 
                        fullPathModelFile=directory+'/ModelController.py', 
                        argsAfter='bgd {}/Model_Inputs/ BGD_network_road_waterway.shp BGD_Districtdata_centroid.shp District_economic_data.xlsx parameter_list.xlsx'.format(directory), 
                        fs_workingDirectory=directory, 
                        redirectStdin='', 
                        redirectStdout=directory+"/out.txt",
                        redirectStderr=directory+"/err.txt",
                        deleteWorkingDirectory=False, deleteStdin=False, deleteStdout=False, deleteStderr=False,
                        ip=ip, 
                        fs_port=fs_port, 
                        fs_receiver="FS", 
                        m_receiver="bgd", 
                        magic_no="SIM01", 
                        sim_run_id="FM", 
                        sender_id="EMA",
                        local_directory=directory)
       

    
    socioeconomic_parameters = list(itertools.product(goods, activity))
    socioeconomic_parameters = [pair[0]+'_'+pair[1] for pair in socioeconomic_parameters]
#     socioeconomic_unc = [RealParameter(prm, boundaries.loc[prm, 'Lower bound'], boundaries.loc[prm, 'Upper bound']) for prm in socioeconomic_parameters]
    
    transport_parameters = list(itertools.product(goods, modes))
    transport_parameters = [pair[0]+'_'+pair[1] for pair in transport_parameters]
    #make sure they add up to 1
#     transport_unc = [RealParameter(prm, boundaries.loc[prm, 'Lower bound'], boundaries.loc[prm, 'Upper bound']) for prm in transport_parameters] 
    
    damage_parameters = {}
    for infra in infrastructure:
        if infra in ['road', 'bridge', 'waterway']:
            dmg_parameters = list(itertools.product(categories[infra], fnc_parameters))
            dmg_parameters = [pair[0]+'_'+pair[1] for pair in dmg_parameters]   
        else:
            dmg_parameters = [infra+'_'+prm for prm in fnc_parameters]
        damage_parameters[infra] = dmg_parameters

    damage_uncertainties = []
    for infra in infrastructure:
        parameters = damage_parameters[infra]
        for parameter in parameters:
            damage_uncertainties.append(RealParameter(parameter, damage_boundaries.loc[parameter, 'Lower'], damage_boundaries.loc[parameter, 'Upper']))
            
    other_unc = [CategoricalParameter('Flood_area', flood_ids),
                 RealParameter('Flood_duration', 1, 90),
                 RealParameter('Flood_depth', 0, 5),
#                  RealParameter('total_import', boundaries.loc['total_import', 'Lower bound'], boundaries.loc['total_import', 'Upper bound']),
#                  RealParameter('water_cost', boundaries.loc['water_cost', 'Lower bound'], boundaries.loc['water_cost', 'Upper bound']),
#                  RealParameter('road_cost', boundaries.loc['road_cost', 'Lower bound'], boundaries.loc['road_cost', 'Upper bound']),
#                  RealParameter('trs_cost', boundaries.loc['trs_cost', 'Lower bound'], boundaries.loc['trs_cost', 'Upper bound'])
                 ]
       
#     model.uncertainties = socioeconomic_unc + transport_unc \
#                         + damage_uncertainties \
#                         + other_unc
    model.uncertainties = other_unc
                        
#     THESE ARE THE CONSTANTS OF THE SIMULATION MODEL, BUT THEY ARE NOT USED I NTHE STATIC MODEL
#     model.constants = [Constant('runtime', 100.0),
#                        Constant('warmupTime', 0.0),
#                        Constant('speed', float("inf")),
#                        Constant('startTime', 0.0),
#                        Constant('noReplications', int(1)),
#                        Constant('seed', 1234)]
                        
    # THESE ARE THE OUTCOME INDICATORS IF THE DISTINCTION BETWEEN THE GOODS IS TAKEN INTO ACCOUNT, E.G. IN THE SIMULATION MODEL, BUT NOT IN THIS VERSION OF THE STATIC MODEL
    #model.outcomes = [ScalarOutcome("{}_TransportCost".format(good)) for good in goods]
    #model.outcomes.extend([ScalarOutcome("{}_TravelTime".format(good)) for good in goods])
    #model.outcomes.extend([ScalarOutcome("{}_UnsatisfiedDemand".format(good)) for good in goods])
    
    model.outcomes = [ScalarOutcome("TransportCost", kind=ScalarOutcome.MINIMIZE),
                      ScalarOutcome("TravelTime", kind=ScalarOutcome.MINIMIZE),
                      ScalarOutcome("UnsatisfiedDemand", kind=ScalarOutcome.MINIMIZE)]
    
    results = perform_experiments(model, 2)
    save_results(results, 'BGD_Policy_2.tar.gz')
    
    #results = perform_experiments(model, 2)
    #experiments, outcomes = results
    #print(outcomes)
