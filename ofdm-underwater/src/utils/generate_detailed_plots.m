function generate_detailed_plots(tx_signal, rx_signal, tx_bits, rx_bits, params, output_dir, snr)
% GENERATE_DETAILED_PLOTS - 生成详细图表并保存到本地
% 输入:
%   tx_signal - 发射信号
%   rx_signal - 接收信号
%   tx_bits - 发射比特
%   rx_bits - 接收比特
%   params - 调制参数
%   output_dir - 输出目录
%   snr - 信噪比

    fprintf('生成详细图表...\n');
    
    image_dir = fullfile(output_dir, 'images');
    
    % 1. 时域信号对比
    fig1 = figure('Visible', 'off', 'Position', [100, 100, 1200, 800]);
    
    % 选择显示的部分信号（避免显示过多数据）
    display_samples = min(1000, length(tx_signal));
    
    subplot(2, 3, 1);
    plot(real(tx_signal(1:display_samples)), 'b-', 'LineWidth', 1.5);
    title('发射信号时域 (实部)');
    xlabel('样本点');
    ylabel('幅度');
    grid on;
    
    subplot(2, 3, 2);
    plot(real(rx_signal(1:min(display_samples, length(rx_signal)))), 'r-', 'LineWidth', 1.5);
    title(['接收信号时域 (实部), SNR = ', num2str(snr), ' dB']);
    xlabel('样本点');
    ylabel('幅度');
    grid on;
    
    % 2. 频域信号对比
    subplot(2, 3, 3);
    fft_length = min(params.ifft_length, length(tx_signal));
    fft_tx = abs(fft(tx_signal(1:fft_length)));
    plot(fft_tx, 'b-', 'LineWidth', 1.5);
    title('发射信号频谱');
    xlabel('频率点');
    ylabel('幅度');
    grid on;
    
    subplot(2, 3, 4);
    fft_rx = abs(fft(rx_signal(1:min(fft_length, length(rx_signal)))));
    plot(fft_rx, 'r-', 'LineWidth', 1.5);
    title('接收信号频谱');
    xlabel('频率点');
    ylabel('幅度');
    grid on;
    
    % 3. 星座图
    subplot(2, 3, 5);
    const_samples = min(1000, length(tx_signal));
    scatter(real(tx_signal(1:const_samples)), ...
            imag(tx_signal(1:const_samples)), 20, 'filled', 'b', 'AlphaData', 0.6);
    title('发射信号星座图');
    xlabel('同相分量 (I)');
    ylabel('正交分量 (Q)');
    axis equal;
    grid on;
    
    subplot(2, 3, 6);
    rx_const_samples = min(1000, length(rx_signal));
    scatter(real(rx_signal(1:rx_const_samples)), ...
            imag(rx_signal(1:rx_const_samples)), 20, 'filled', 'r', 'AlphaData', 0.6);
    title('接收信号星座图');
    xlabel('同相分量 (I)');
    ylabel('正交分量 (Q)');
    axis equal;
    grid on;
    
    % 保存图片
    saveas(fig1, fullfile(image_dir, 'time_frequency_constellation.png'));
    close(fig1);
    fprintf('保存时频域和星座图: time_frequency_constellation.png\n');
    
    % 4. 生成BER随时间变化图（如果有多组数据）
    generate_additional_plots(tx_signal, rx_signal, tx_bits, rx_bits, params, output_dir, snr);
    
    % 保存图表数据
    save_plot_data(tx_signal, rx_signal, params, output_dir);
end

function generate_additional_plots(tx_signal, rx_signal, tx_bits, rx_bits, params, output_dir, snr)
% 生成额外的图表
    
    image_dir = fullfile(output_dir, 'images');
    
    % 信号功率谱密度对比
    fig2 = figure('Visible', 'off', 'Position', [100, 100, 1000, 600]);
    
    subplot(2, 2, 1);
    [pxx_tx, f_tx] = pwelch(tx_signal, [], [], [], 1, 'centered');
    plot(f_tx, 10*log10(pxx_tx), 'b-', 'LineWidth', 1.5);
    title('发射信号功率谱密度');
    xlabel('归一化频率');
    ylabel('功率谱密度 (dB/Hz)');
    grid on;
    
    subplot(2, 2, 2);
    [pxx_rx, f_rx] = pwelch(rx_signal, [], [], [], 1, 'centered');
    plot(f_rx, 10*log10(pxx_rx), 'r-', 'LineWidth', 1.5);
    title('接收信号功率谱密度');
    xlabel('归一化频率');
    ylabel('功率谱密度 (dB/Hz)');
    grid on;
    
    % 信号幅度分布
    subplot(2, 2, 3);
    histogram(abs(tx_signal), 50, 'FaceColor', 'b', 'FaceAlpha', 0.7);
    title('发射信号幅度分布');
    xlabel('幅度');
    ylabel('频数');
    grid on;
    
    subplot(2, 2, 4);
    histogram(abs(rx_signal), 50, 'FaceColor', 'r', 'FaceAlpha', 0.7);
    title('接收信号幅度分布');
    xlabel('幅度');
    ylabel('频数');
    grid on;
    
    saveas(fig2, fullfile(image_dir, 'signal_analysis.png'));
    close(fig2);
    fprintf('保存信号分析图: signal_analysis.png\n');
end

function save_plot_data(tx_signal, rx_signal, params, output_dir)
% 保存绘图数据
    
    data_dir = fullfile(output_dir, 'data');
    
    % 保存时域数据（采样部分数据以避免文件过大）
    max_samples = min(1000, length(tx_signal));
    
    plot_data.tx_time_real = real(tx_signal(1:max_samples));
    plot_data.rx_time_real = real(rx_signal(1:min(max_samples, length(rx_signal))));
    
    % 保存频域数据
    fft_length = min(params.ifft_length, length(tx_signal));
    plot_data.tx_freq = abs(fft(tx_signal(1:fft_length)));
    plot_data.rx_freq = abs(fft(rx_signal(1:min(fft_length, length(rx_signal)))));
    
    % 保存到MAT文件
    save(fullfile(data_dir, 'plot_data.mat'), 'plot_data');
    
    % 保存到CSV文件（用于其他分析）
    time_data = table((1:length(plot_data.tx_time_real))', plot_data.tx_time_real, ...
                      plot_data.rx_time_real(1:length(plot_data.tx_time_real)), ...
                      'VariableNames', {'Sample', 'Tx_Signal', 'Rx_Signal'});
    writetable(time_data, fullfile(data_dir, 'time_domain_data.csv'));
    
    freq_data = table((1:length(plot_data.tx_freq))', plot_data.tx_freq, ...
                      plot_data.rx_freq(1:length(plot_data.tx_freq)), ...
                      'VariableNames', {'Frequency_Bin', 'Tx_Spectrum', 'Rx_Spectrum'});
    writetable(freq_data, fullfile(data_dir, 'frequency_domain_data.csv'));
    
    fprintf('绘图数据已保存到: %s\n', data_dir);
end