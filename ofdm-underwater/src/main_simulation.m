function main_simulation()
    % OFDM水声通信系统主仿真
    
    init_project();
    try
    % 加载配置
    config = underwater_config();
    config.enable_freq_domain_equalization = true;
    
    validate_config(config);

    % 生成随机比特序列
    bit_length = config.carrier_count * config.symbol_count * config.bit_per_symbol;
    bit_sequence = randi([0 1], bit_length, 1);
    
    fprintf('开始OFDM调制...\n');
    % OFDM调制
    [tx_signal, mod_params] = ofdm_modulator(bit_sequence, config);  
    fprintf('调制完成，信号长度: %d\n', length(tx_signal));
        
    fprintf('通过水声信道...\n');
    % 通过水声信道
    config.SNR = 20; % 设置SNR
    [rx_signal, channel_info] = underwater_channel(tx_signal, config);
    fprintf('信道传输完成，接收信号长度: %d\n', length(rx_signal));

    fprintf('添加帧同步...\n');
    rx_signal = add_cp_synchronization(rx_signal, config.CP_length, config.ifft_length);
    
    % 确保信号长度是符号长度的整数倍
    symbol_length = config.ifft_length + config.CP_length + config.CS_length;
    num_symbols = floor(length(rx_signal) / symbol_length);
    rx_signal = rx_signal(1:num_symbols*symbol_length);
    fprintf('同步后信号长度: %d, 完整符号数: %d\n', length(rx_signal), num_symbols);

    % 时域均衡 (可选)
    if config.enable_time_domain_equalization
        % 使用前100个符号作为训练序列
        training_length = min(100, length(tx_signal));
        training_seq = tx_signal(1:training_length);
        
        switch config.equalizer_type
            case 'LMS'
                [rx_signal, ~, ~] = lms_equalizer(...
                    rx_signal, training_seq, config.step_size, config.equalizer_length);
            case 'RLS'
                [rx_signal, ~, ~] = rls_equalizer(...
                    rx_signal, training_seq, config.forgetting_factor, config.equalizer_length);
            otherwise
                warning('使用默认频域均衡');
        end
    end
    
    % OFDM解调
    [rx_bit_sequence, equalizer_info] = ofdm_demodulator(rx_signal, mod_params, config, channel_info);
    
    if isempty(fieldnames(equalizer_info))
        fprintf('警告: 均衡器信息为空\n');
    else
        fprintf('均衡器方法: %s\n', equalizer_info.method);
    end
    
    % 调整期望的比特数
    expected_bits = config.carrier_count * config.symbol_count * config.bit_per_symbol;
    if length(rx_bit_sequence) ~= expected_bits
        fprintf('警告: 解调比特数%d不等于期望比特数%d\n', length(rx_bit_sequence), expected_bits);
        % 截断或填充
        if length(rx_bit_sequence) > expected_bits
            rx_bit_sequence = rx_bit_sequence(1:expected_bits);
        else
            rx_bit_sequence = [rx_bit_sequence; zeros(expected_bits - length(rx_bit_sequence), 1)];
        end
    end
    
    % 计算误码率
    ber = sum(rx_bit_sequence ~= bit_sequence) / bit_length;
    fprintf('BER: %.4f (SNR: %d dB)\n', ber, config.SNR);
    
    % 绘制结果
    if config.plot_results
        plot_results(tx_signal, rx_signal, bit_sequence, rx_bit_sequence, mod_params);
    end
    catch ME
        fprintf('错误: %s\n', ME.message);
        fprintf('请检查配置文件路径\n');
    end
end

function plot_results(tx_signal, rx_signal, tx_bits, rx_bits, params)
    % 绘制仿真结果
    
    figure;
    
    % 时域信号对比
    subplot(3,2,1);
    plot(real(tx_signal(1:1000)));
    title('发射信号 (实部)');
    xlabel('样本');
    ylabel('幅度');
    
    subplot(3,2,2);
    plot(real(rx_signal(1:1000)));
    title('接收信号 (实部)');
    xlabel('样本');
    ylabel('幅度');
    
    % 频域信号对比
    subplot(3,2,3);
    fft_tx = abs(fft(tx_signal(1:params.ifft_length)));
    plot(fft_tx);
    title('发射信号频谱');
    xlabel('频率');
    ylabel('幅度');
    
    subplot(3,2,4);
    fft_rx = abs(fft(rx_signal(1:params.ifft_length)));
    plot(fft_rx);
    title('接收信号频谱');
    xlabel('频率');
    ylabel('幅度');
    
    % 星座图
    subplot(3,2,5);
    scatter(real(tx_signal), imag(tx_signal), '.');
    title('发射信号星座图');
    xlabel('同相分量');
    ylabel('正交分量');
    axis square;
    
    subplot(3,2,6);
    scatter(real(rx_signal), imag(rx_signal), '.');
    title('接收信号星座图');
    xlabel('同相分量');
    ylabel('正交分量');
    axis square;
    
    % BER性能
    figure;
    semilogy(0:5:30, [0.5 0.2 0.1 0.05 0.01 0.005 0.001]);
    title('BER vs SNR');
    xlabel('SNR (dB)');
    ylabel('误码率');
    grid on;
end