function test_equalizers()
    % 均衡器性能比较测试
    
    % 加载配置
    config = underwater_config();
    
    % 生成测试数据
    bit_length = config.carrier_count * 10 * config.bit_per_symbol; % 10个符号
    test_bits = randi([0 1], bit_length, 1);
    
    % OFDM调制
    [tx_signal, mod_params] = ofdm_modulator(test_bits, config);
    
    % 通过水声信道
    config.SNR = 15;
    [rx_signal, ~] = underwater_channel(tx_signal, config);
    
    % 测试不同均衡器
    equalizer_types = {'LMS', 'RLS', 'MMSE'};
    results = struct();
    
    for i = 1:length(equalizer_types)
        eq_type = equalizer_types{i};
        fprintf('\n测试均衡器: %s\n', eq_type);
        
        % 复制配置
        current_config = config;
        current_config.equalizer_type = eq_type;
        
        % 使用前20%作为训练序列
        training_length = floor(0.2 * length(tx_signal));
        training_seq = tx_signal(1:training_length);
        
        % 应用均衡器
        switch eq_type
            case 'LMS'
                [eq_signal, ~, ~] = lms_equalizer(...
                    rx_signal, training_seq, current_config.step_size, current_config.equalizer_length);
                
            case 'RLS'
                [eq_signal, ~, ~] = rls_equalizer(...
                    rx_signal, training_seq, current_config.forgetting_factor, current_config.equalizer_length);
                
            case 'MMSE'
                % 使用前两个符号作为导频
                pilot_symbols = tx_signal(1:mod_params.ifft_length);
                eq_signal = mmse_equalizer(...
                    rx_signal, pilot_symbols, 1:mod_params.ifft_length, current_config.SNR, mod_params.ifft_length);
        end
        
        % OFDM解调
        [rx_bits, ~] = ofdm_demodulator(eq_signal, mod_params, current_config, []);
        
        % 计算性能指标
        [ber, ser, evm] = calculate_metrics(tx_signal, eq_signal, test_bits, rx_bits, 16);
        
        % 保存结果
        results.(eq_type).ber = ber;
        results.(eq_type).ser = ser;
        results.(eq_type).evm = evm;
    end
    
    % 显示比较结果
    fprintf('\n均衡器性能比较:\n');
    fprintf('%-8s %-8s %-8s %-8s\n', '均衡器', 'BER', 'SER', 'EVM');
    for i = 1:length(equalizer_types)
        eq_type = equalizer_types{i};
        fprintf('%-8s %.4f   %.4f   %.4f\n', ...
            eq_type, results.(eq_type).ber, results.(eq_type).ser, results.(eq_type).evm);
    end
    
    % 绘制星座图比较
    figure;
    for i = 1:length(equalizer_types)
        subplot(1,3,i);
        eq_type = equalizer_types{i};
        scatter(real(eq_signal), imag(eq_signal), '.');
        title(sprintf('%s均衡后星座图', eq_type));
        xlabel('同相分量');
        ylabel('正交分量');
        axis square;
    end
end