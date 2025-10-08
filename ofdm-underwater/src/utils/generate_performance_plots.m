function generate_performance_plots(snr_range, ber_results, ser_results, evm_results, output_dir)
% GENERATE_PERFORMANCE_PLOTS - 生成性能曲线图
    
    fprintf('生成性能曲线图...\n');
    
    image_dir = fullfile(output_dir, 'images');
    
    % BER曲线
    fig1 = figure('Visible', 'off', 'Position', [100, 100, 800, 600]);
    semilogy(snr_range, ber_results, 'bo-', 'LineWidth', 2, 'MarkerSize', 8, 'MarkerFaceColor', 'b');
    grid on;
    title('BER vs SNR');
    xlabel('SNR (dB)');
    ylabel('误码率 (BER)');
    set(gca, 'YScale', 'log');
    saveas(fig1, fullfile(image_dir, 'ber_curve.png'));
    close(fig1);
    fprintf('保存BER曲线: ber_curve.png\n');
    
    % SER曲线
    fig2 = figure('Visible', 'off', 'Position', [100, 100, 800, 600]);
    semilogy(snr_range, ser_results, 'ro-', 'LineWidth', 2, 'MarkerSize', 8, 'MarkerFaceColor', 'r');
    grid on;
    title('SER vs SNR');
    xlabel('SNR (dB)');
    ylabel('误符号率 (SER)');
    set(gca, 'YScale', 'log');
    saveas(fig2, fullfile(image_dir, 'ser_curve.png'));
    close(fig2);
    fprintf('保存SER曲线: ser_curve.png\n');
    
    % EVM曲线
    fig3 = figure('Visible', 'off', 'Position', [100, 100, 800, 600]);
    plot(snr_range, evm_results*100, 'go-', 'LineWidth', 2, 'MarkerSize', 8, 'MarkerFaceColor', 'g');
    grid on;
    title('EVM vs SNR');
    xlabel('SNR (dB)');
    ylabel('EVM (%)');
    saveas(fig3, fullfile(image_dir, 'evm_curve.png'));
    close(fig3);
    fprintf('保存EVM曲线: evm_curve.png\n');
    
    % 综合性能图
    fig4 = figure('Visible', 'off', 'Position', [100, 100, 1000, 800]);
    
    subplot(2, 2, 1);
    semilogy(snr_range, ber_results, 'bo-', 'LineWidth', 2, 'MarkerSize', 6);
    title('BER性能');
    xlabel('SNR (dB)');
    ylabel('BER');
    grid on;
    
    subplot(2, 2, 2);
    semilogy(snr_range, ser_results, 'ro-', 'LineWidth', 2, 'MarkerSize', 6);
    title('SER性能');
    xlabel('SNR (dB)');
    ylabel('SER');
    grid on;
    
    subplot(2, 2, 3);
    plot(snr_range, evm_results*100, 'go-', 'LineWidth', 2, 'MarkerSize', 6);
    title('EVM性能');
    xlabel('SNR (dB)');
    ylabel('EVM (%)');
    grid on;
    
    subplot(2, 2, 4);
    % 计算信道容量（简化）
    capacity = log2(1 + 10.^(snr_range/10));
    plot(snr_range, capacity, 'mo-', 'LineWidth', 2, 'MarkerSize', 6);
    title('理论信道容量');
    xlabel('SNR (dB)');
    ylabel('容量 (bps/Hz)');
    grid on;
    
    saveas(fig4, fullfile(image_dir, 'performance_summary.png'));
    close(fig4);
    fprintf('保存性能汇总图: performance_summary.png\n');
end