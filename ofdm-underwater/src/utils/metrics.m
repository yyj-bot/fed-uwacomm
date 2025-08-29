function [ber, ser, evm] = calculate_metrics(tx_signal, rx_signal, tx_bits, rx_bits, mod_order)
    % 计算通信系统性能指标
    % 输入:
    %   tx_signal - 发射信号
    %   rx_signal - 接收信号
    %   tx_bits - 发射比特
    %   rx_bits - 接收比特
    %   mod_order - 调制阶数
    % 输出:
    %   ber - 误码率
    %   ser - 误符号率
    %   evm - 误差向量幅度
    
    % 误码率(BER)
    ber = sum(tx_bits ~= rx_bits) / length(tx_bits);
    
    % 误符号率(SER)
    tx_symbols = qammod(tx_bits, mod_order, 'InputType', 'bit', 'UnitAveragePower', true);
    rx_symbols = qammod(rx_bits, mod_order, 'InputType', 'bit', 'UnitAveragePower', true);
    ser = sum(tx_symbols ~= rx_symbols) / length(tx_symbols);
    
    % 误差向量幅度(EVM)
    evm = sqrt(mean(abs(tx_signal - rx_signal).^2) / sqrt(mean(abs(tx_signal).^2)));
    
    % 显示结果
    fprintf('性能指标:\n');
    fprintf('误码率(BER): %.4f\n', ber);
    fprintf('误符号率(SER): %.4f\n', ser);
    fprintf('误差向量幅度(EVM): %.4f %%\n', evm*100);
end