function validate_config(config)
    % 验证配置参数的有效性
    
    % 检查必要的字段是否存在
    required_fields = {
        'carrier_count', 'symbol_count', 'ifft_length',...
        'CP_length', 'CS_length', 'bit_per_symbol', 'alpha',...
        'sampling_rate', 'sound_speed', 'doppler_shift',...
        'multipath_delays', 'multipath_gains', 'equalizer_type'
    };
    
    for i = 1:length(required_fields)
        if ~isfield(config, required_fields{i})
            error('缺少必需的配置字段: %s', required_fields{i});
        end
    end
    
    % 检查数值有效性
    if config.carrier_count <= 0 || mod(config.carrier_count, 1) ~= 0
        error('carrier_count必须是正整数');
    end
    
    if config.ifft_length <= config.carrier_count
        error('IFFT长度必须大于子载波数');
    end
    
    % 检查多径参数
    if length(config.multipath_delays) ~= length(config.multipath_gains)
        error('multipath_delays和multipath_gains数组长度必须相同');
    end
    
    if any(config.multipath_delays < 0)
        error('多径时延不能为负数');
    end
    
    if any(config.multipath_gains < 0)
        error('多径增益不能为负数');
    end
    
    fprintf('配置验证通过\n');
end