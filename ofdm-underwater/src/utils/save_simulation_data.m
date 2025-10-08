function save_simulation_data(snr_range, ber_results, ser_results, evm_results, output_dir)
% SAVE_SIMULATION_DATA - 保存仿真数据
    
    fprintf('保存仿真数据...\n');
    
    data_dir = fullfile(output_dir, 'data');
    
    % 保存性能数据到CSV
    performance_data = table(snr_range', ber_results', ser_results', evm_results', ...
        'VariableNames', {'SNR_dB', 'BER', 'SER', 'EVM'});
    writetable(performance_data, fullfile(data_dir, 'performance_data.csv'));
    
    % 保存到MAT文件
    save(fullfile(data_dir, 'simulation_results.mat'), ...
        'snr_range', 'ber_results', 'ser_results', 'evm_results');
    
    % 生成JSON格式数据（用于前端）
    generate_json_data(snr_range, ber_results, ser_results, evm_results, data_dir);
    
    fprintf('仿真数据已保存到: %s\n', data_dir);
end

function generate_json_data(snr_range, ber_results, ser_results, evm_results, data_dir)
% 生成JSON格式数据用于前端显示
    
    json_data = struct();
    json_data.snr_range = snr_range;
    json_data.ber = ber_results;
    json_data.ser = ser_results;
    json_data.evm = evm_results;
    json_data.timestamp = datestr(now);
    
    json_str = jsonencode(json_data);
    
    fid = fopen(fullfile(data_dir, 'performance_data.json'), 'w');
    if fid ~= -1
        fprintf(fid, '%s', json_str);
        fclose(fid);
        fprintf('JSON数据已生成: performance_data.json\n');
    else
        warning('无法创建JSON文件');
    end
end