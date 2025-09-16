function init_project()
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
    
    fprintf('项目初始化完成\n');
    fprintf('项目根目录: %s\n', project_root);
end