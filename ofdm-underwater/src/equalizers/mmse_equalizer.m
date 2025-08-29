function [equalized_signal, H_est] = mmse_equalizer(rx_signal, pilot_symbols, pilot_positions, snr_db, ifft_length)
    % 频域MMSE均衡器
    % 输入:
    %   rx_signal - 接收信号
    %   pilot_symbols - 导频符号
    %   pilot_positions - 导频位置
    %   snr_db - 信噪比(dB)
    %   ifft_length - FFT长度
    % 输出:
    %   equalized_signal - 均衡后信号
    %   H_est - 信道估计
    
    % 转换为频域
    rx_freq = fft(rx_signal, ifft_length);
    
    % 信道估计 (使用导频)
    H_est = zeros(ifft_length, 1);
    H_pilots = rx_freq(pilot_positions) ./ pilot_symbols;
    
    % 插值获取完整信道响应
    H_est(pilot_positions) = H_pilots;
    non_pilot = setdiff(1:ifft_length, pilot_positions);
    H_est(non_pilot) = interp1(pilot_positions, H_pilots, non_pilot, 'spline');
    
    % MMSE均衡
    snr_linear = 10^(snr_db/10);
    W_mmse = conj(H_est) ./ (abs(H_est).^2 + 1/snr_linear);
    equalized_signal = rx_freq .* W_mmse;
    
    % 转换回时域
    equalized_signal = ifft(equalized_signal);
end