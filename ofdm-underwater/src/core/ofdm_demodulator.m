function [bit_sequence, equalizer_info] = ofdm_demodulator(rx_signal, params, config, channel_info)
    fprintf('=== OFDM解调开始 ===\n');
    fprintf('输入信号长度: %d\n', length(rx_signal));
    
    % 初始化equalizer_info为空结构体
    equalizer_info = struct();
    
    % 计算每个符号的完整长度
    symbol_full_length = params.ifft_length + params.CP_length + params.CS_length;
    
    % 计算实际符号数
    actual_symbols = floor(length(rx_signal) / symbol_full_length);
    fprintf('实际符号数: %d, 符号完整长度: %d\n', actual_symbols, symbol_full_length);
    
    % 截取完整符号
    rx_signal = rx_signal(1:actual_symbols * symbol_full_length);
    
    % 串并转换
    rx_matrix = reshape(rx_signal, symbol_full_length, actual_symbols);
    fprintf('串并转换后矩阵尺寸: %s\n', mat2str(size(rx_matrix)));
    
    % 去除循环前缀和后缀
    rx_matrix(1:params.CP_length, :) = [];
    rx_matrix(end-params.CS_length+1:end, :) = [];
    fprintf('去CP/CS后矩阵尺寸: %s\n', mat2str(size(rx_matrix)));
    
    % FFT变换
    fft_symbols = fft(rx_matrix, params.ifft_length);
    fprintf('FFT后矩阵尺寸: %s\n', mat2str(size(fft_symbols)));
    
    % 频域均衡
    if config.enable_freq_domain_equalization
        [equalized_freq, equalizer_info] = frequency_domain_equalization(fft_symbols, params, config);
        fprintf('频域均衡完成\n');
    else
        equalized_freq = fft_symbols;
        equalizer_info.method = 'none';
        fprintf('跳过频域均衡\n');
    end
    
    % 提取数据子载波
    data_symbols = equalized_freq(params.carrier_position, :);
    fprintf('数据子载波尺寸: %s\n', mat2str(size(data_symbols)));
    
    % 16QAM解调
    bit_sequence = qamdemod(data_symbols(:), 16, 'OutputType', 'bit', 'UnitAveragePower', true);
    fprintf('解调完成，比特数: %d\n', length(bit_sequence));
    
    % 确保equalizer_info始终有值
    if isempty(fieldnames(equalizer_info))
        equalizer_info.status = 'no_equalization';
        equalizer_info.timestamp = datetime;
    end
end

function [equalized_freq, info] = frequency_domain_equalization(fft_symbols, params, config)
    % 确保总是返回info结构体
    info = struct();
    
    % 假设前两个符号为导频符号
    pilot_symbols = fft_symbols(:, 1:2);
    fprintf('导频符号尺寸: %s\n', mat2str(size(pilot_symbols)));
    
    % 信道估计 (简化版)
    H_est = mean(pilot_symbols(:, 1:2), 2);
    info.H_est = H_est;
    
    % 选择均衡算法
    equalizer_type = upper(config.equalizer_type);
    
    switch equalizer_type
        case {'LMS', 'RLS'}
            % 这些是时域均衡器，在频域中使用ZF代替
            fprintf('警告: %s是时域均衡器，在频域中使用ZF代替\n', config.equalizer_type);
            equalized_freq = fft_symbols ./ (H_est + eps);
            info.method = 'ZF';
            info.original_type = config.equalizer_type;
            
        case 'MMSE'
            % MMSE均衡
            SNR_linear = 10^(config.SNR/10);
            equalized_freq = fft_symbols .* conj(H_est) ./ (abs(H_est).^2 + 1/SNR_linear);
            info.method = 'MMSE';
            info.SNR_linear = SNR_linear;
            
        case 'ZF'
            % 迫零均衡
            equalized_freq = fft_symbols ./ (H_est + eps);
            info.method = 'ZF';
            
        otherwise
            fprintf('警告: 未知均衡器类型%s，使用ZF均衡\n', config.equalizer_type);
            equalized_freq = fft_symbols ./ (H_est + eps);
            info.method = 'ZF';
            info.original_type = config.equalizer_type;
    end
    
    info.equalizer_type = config.equalizer_type;
    info.timestamp = datetime;
end