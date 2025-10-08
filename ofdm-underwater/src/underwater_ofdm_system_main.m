function [results, image_paths] = underwater_ofdm_system_main(config_params)
% UNDERWATER_OFDM_SYSTEM_MAIN - 主函数，支持Java调用和参数传递
% 输入:
%   config_params - 结构体或包含配置参数的Java对象
% 输出:
%   results - 包含性能指标的结构体
%   image_paths - 生成的图片路径列表

    % 初始化项目
    init_project();
    
    try
        % 处理输入参数
        if nargin == 0
            % 如果没有输入参数，使用默认配置
            config = underwater_config();
        else
            % 转换Java对象或使用提供的结构体
            config = process_input_parameters(config_params);
        end
        
        % 验证配置
        validate_config(config);
        
        % 创建输出目录
        output_dir = create_output_directory();
        
        % 运行主仿真
        [ber_results, ser_results, evm_results, snr_range] = run_main_simulation(config, output_dir);
        
        % 准备输出结果
        results = struct();
        results.ber = ber_results;
        results.ser = ser_results;
        results.evm = evm_results;
        results.snr_range = snr_range;
        results.timestamp = datestr(now);
        
        % 获取生成的图片路径
        image_paths = get_generated_image_paths(output_dir);
        
        fprintf('仿真完成！\n');
        fprintf('输出目录: %s\n', output_dir);
        
    catch ME
        fprintf('错误: %s\n', ME.message);
        rethrow(ME);
    end
end

function config = process_input_parameters(params)
% 处理输入参数，支持Java对象或MATLAB结构体
    if isjava(params)
        % 如果是Java对象，转换为MATLAB结构体
        config = java_object_to_struct(params);
    elseif isstruct(params)
        % 如果是结构体，直接使用
        config = params;
    else
        error('不支持的参数类型');
    end
    
    % 设置默认值（如果某些字段缺失）
    config = set_default_config(config);
end

function config = java_object_to_struct(java_obj)
% 将Java对象转换为MATLAB结构体
    config = struct();
    
    % 获取所有方法
    methods = methods(java_obj);
    
    for i = 1:length(methods)
        method_name = methods{i};
        
        % 检查是否是getter方法
        if startsWith(method_name, 'get') && length(method_name) > 3
            field_name = lower(method_name(4:end));
            
            try
                % 调用getter方法获取值
                value = java_obj.(method_name)();
                
                % 转换Java数组为MATLAB数组
                if isjava(value) && value.getClass().isArray()
                    value = java_array_to_matlab(value);
                end
                
                config.(field_name) = value;
            catch
                % 忽略无法访问的属性
            end
        end
    end
end

function matlab_array = java_array_to_matlab(java_array)
% 将Java数组转换为MATLAB数组
    if isjava(java_array)
        len = java_array.length;
        matlab_array = zeros(1, len);
        for j = 1:len
            matlab_array(j) = java_array(j);
        end
    else
        matlab_array = java_array;
    end
end

function config = set_default_config(config)
% 设置默认配置值
    default_config = underwater_config();
    
    fields = fieldnames(default_config);
    for i = 1:length(fields)
        field = fields{i};
        if ~isfield(config, field)
            config.(field) = default_config.(field);
        end
    end
end

function output_dir = create_output_directory()
% 创建输出目录
    base_dir = 'output_results';
    if ~exist(base_dir, 'dir')
        mkdir(base_dir);
    end
    
    timestamp = datestr(now, 'yyyy-mm-dd_HH-MM-SS');
    output_dir = fullfile(base_dir, ['simulation_', timestamp]);
    mkdir(output_dir);
    
    % 创建图片子目录
    image_dir = fullfile(output_dir, 'images');
    mkdir(image_dir);
    
    % 创建数据子目录
    data_dir = fullfile(output_dir, 'data');
    mkdir(data_dir);
end

function image_paths = get_generated_image_paths(output_dir)
% GET_GENERATED_IMAGE_PATHS - 获取生成的图片路径列表
% 输入:
%   output_dir - 输出目录
% 输出:
%   image_paths - 图片路径的单元格数组

    image_dir = fullfile(output_dir, 'images');
    
    % 检查图片目录是否存在
    if ~exist(image_dir, 'dir')
        fprintf('警告: 图片目录不存在: %s\n', image_dir);
        image_paths = {};
        return;
    end
    
    % 获取所有PNG文件
    image_files = dir(fullfile(image_dir, '*.png'));
    
    if isempty(image_files)
        fprintf('警告: 未找到PNG图片文件\n');
        image_paths = {};
        return;
    end
    
    % 构建完整路径
    image_paths = cell(length(image_files), 1);
    for i = 1:length(image_files)
        image_paths{i} = fullfile(image_dir, image_files(i).name);
    end
    
    fprintf('找到 %d 个图片文件\n', length(image_paths));
    
    % 显示找到的图片
    for i = 1:length(image_paths)
        fprintf('  %d. %s\n', i, image_paths{i});
    end
end