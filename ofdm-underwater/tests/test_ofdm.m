function test_ofdm()
    % OFDM系统测试脚本
    
    % 加载配置
    config = underwater_config();
    
    % 测试不同SNR下的性能
    snr_range = config.SNR_range;
    ber_results = zeros(size(snr_range));
    
    for i = 1:length(snr_range)
        % 设置当前SNR
        config.SNR = snr_range(i);
        
        % 生成测试数据
        bit_length = config.carrier_count * config.symbol_count * config.bit_per_symbol;
        test_bits = randi([0 1], bit_length, 1);
        
        % OFDM调制
        [tx_signal, mod_params] = ofdm_modulator(test_bits, config);
        
        % 通过信道
        [rx_signal, ~] = underwater_channel(tx_signal, config);
        
        % OFDM解调
        [rx_bits, ~] = ofdm_demodulator(rx_signal, mod_params, config, []);
        
        % 计算BER
        ber_results(i) = sum(rx_bits ~= test_bits) / bit_length;
    end
    
    % 绘制BER曲线
    figure;
    semilogy(snr_range, ber_results, '-o');
    grid on;
    xlabel('SNR (dB)');
    ylabel('误码率 (BER)');
    title('OFDM系统性能测试');
end