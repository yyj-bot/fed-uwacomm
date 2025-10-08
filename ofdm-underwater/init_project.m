function init_project()
% INIT_PROJECT - 初始化项目，确保所有函数都在路径中

    % 清空工作区和图形
    clear all; close all; clc;
    
    % 获取项目根目录
    project_root = fileparts(mfilename('fullpath'));
    
    % 移除可能存在的旧路径
    warning off;
    rmpath(genpath(project_root));
    warning on;
    
    % 添加项目路径
    addpath(genpath(project_root));
    
    % 检查关键函数是否存在
    critical_functions = {
        'calculate_metrics', 'underwater_config', 'ofdm_modulator', ...
        'ofdm_demodulator', 'underwater_channel', 'add_cp_synchronization'
    };
    
    fprintf('检查关键函数...\n');
    for i = 1:length(critical_functions)
        func_path = which(critical_functions{i});
        if isempty(func_path)
            warning('未找到关键函数: %s', critical_functions{i});
        else
            fprintf('✓ 找到: %s\n', critical_functions{i});
        end
    end
    
    fprintf('项目初始化完成\n');
    fprintf('项目根目录: %s\n', project_root);
end