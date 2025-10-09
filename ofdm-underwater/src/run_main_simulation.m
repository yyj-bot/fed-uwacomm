function [ber_results, ser_results, evm_results, snr_range] = run_main_simulation(config, output_dir)
% 运行主仿真，支持参数配置和结果保存

    % 提取配置参数
    snr_range = config.SNR_range;
    enable_freq_domain_equalization = config.enable_freq_domain_equalization;
    
    % 初始化结果数组
    ber_results = zeros(size(snr_range));
    ser_results = zeros(size(snr_range));
    evm_results = zeros(size(snr_range));
    
    % 生成随机比特序列
    bit_length = config.carrier_count * config.symbol_count * config.bit_per_symbol;
    original_bits = randi([0 1], bit_length, 1);
    
    fprintf('开始OFDM系统仿真...\n');
    fprintf('SNR范围: %s dB\n', mat2str(snr_range));
    
    % 对每个SNR进行仿真
    for snr_idx = 1:length(snr_range)
        current_snr = snr_range(snr_idx);
        fprintf('处理 SNR = %d dB...\n', current_snr);
        
        % 设置当前SNR
        config.SNR = current_snr;
        
        % OFDM调制
        [tx_signal, mod_params] = ofdm_modulator(original_bits, config);
        
        % 通过水声信道
        [rx_signal, channel_info] = underwater_channel(tx_signal, config);
        
        % 添加帧同步
        rx_signal = add_cp_synchronization(rx_signal, config.CP_length, config.ifft_length);
        
        % 调整信号长度
        symbol_length = config.ifft_length + config.CP_length + config.CS_length;
        num_symbols = floor(length(rx_signal) / symbol_length);
        rx_signal = rx_signal(1:num_symbols*symbol_length);
        
        % OFDM解调
        [rx_bits, equalizer_info] = ofdm_demodulator(rx_signal, mod_params, config, channel_info);
        
        % 调整比特序列长度
        expected_bits = config.carrier_count * config.symbol_count * config.bit_per_symbol;
        if length(rx_bits) ~= expected_bits
            if length(rx_bits) > expected_bits
                rx_bits = rx_bits(1:expected_bits);
            else
                rx_bits = [rx_bits; zeros(expected_bits - length(rx_bits), 1)];
            end
        end
        
        % 计算性能指标
        [ber, ser, evm] = calculate_metrics(tx_signal, rx_signal, original_bits, rx_bits, 16);
        
        ber_results(snr_idx) = ber;
        ser_results(snr_idx) = ser;
        evm_results(snr_idx) = evm;
        
        % 为第一个SNR点生成详细图表
        if snr_idx == 1
            generate_detailed_plots(tx_signal, rx_signal, original_bits, rx_bits, mod_params, output_dir, current_snr);
        end
    end
    
    % 生成性能曲线
    generate_performance_plots(snr_range, ber_results, ser_results, evm_results, output_dir);
    
    % 保存数据
    save_simulation_data(snr_range, ber_results, ser_results, evm_results, output_dir);
end