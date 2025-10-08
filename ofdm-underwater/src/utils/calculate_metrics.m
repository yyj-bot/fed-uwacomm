function [ber, ser, evm] = calculate_metrics(tx_signal, rx_signal, tx_bits, rx_bits, mod_order)
% CALCULATE_METRICS - 计算通信系统性能指标
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

    fprintf('计算性能指标...\n');
    fprintf('tx_signal 尺寸: %s\n', mat2str(size(tx_signal)));
    fprintf('rx_signal 尺寸: %s\n', mat2str(size(rx_signal)));
    fprintf('tx_bits 尺寸: %s\n', mat2str(size(tx_bits)));
    fprintf('rx_bits 尺寸: %s\n', mat2str(size(rx_bits)));
    
    % 确保信号是列向量
    if size(tx_signal, 2) > 1
        tx_signal = tx_signal(:);
    end
    if size(rx_signal, 2) > 1
        rx_signal = rx_signal(:);
    end
    
    % 确保比特序列长度相同
    min_bit_length = min(length(tx_bits), length(rx_bits));
    if min_bit_length == 0
        error('比特序列长度不能为零');
    end
    
    tx_bits_trim = tx_bits(1:min_bit_length);
    rx_bits_trim = rx_bits(1:min_bit_length);
    
    % 误码率(BER)
    ber = sum(tx_bits_trim ~= rx_bits_trim) / min_bit_length;
    
    % 误符号率(SER)
    bits_per_symbol = log2(mod_order);
    num_symbols = floor(min_bit_length / bits_per_symbol);
    
    if num_symbols > 0
        % 重塑为符号矩阵
        tx_symbol_bits = reshape(tx_bits_trim(1:num_symbols*bits_per_symbol), bits_per_symbol, num_symbols)';
        rx_symbol_bits = reshape(rx_bits_trim(1:num_symbols*bits_per_symbol), bits_per_symbol, num_symbols)';
        
        % 计算符号错误
        symbol_errors = 0;
        for i = 1:num_symbols
            if ~isequal(tx_symbol_bits(i,:), rx_symbol_bits(i,:))
                symbol_errors = symbol_errors + 1;
            end
        end
        ser = symbol_errors / num_symbols;
    else
        ser = 1; % 如果没有完整的符号，设为最大错误率
    end
    
    % 误差向量幅度(EVM)
    % 确保信号长度相同
    min_signal_length = min(length(tx_signal), length(rx_signal));
    fprintf('最小信号长度: %d\n', min_signal_length);
    
    if min_signal_length > 0
        tx_signal_trim = tx_signal(1:min_signal_length);
        rx_signal_trim = rx_signal(1:min_signal_length);
        
        % 修复EVM计算公式 - 添加缺失的括号
        signal_power = mean(abs(tx_signal_trim).^2);
        if signal_power > 0
            error_power = mean(abs(tx_signal_trim - rx_signal_trim).^2);
            evm = sqrt(error_power / signal_power);
        else
            evm = 1; % 如果信号功率为零，设为最大EVM
        end
    else
        evm = 1; % 如果信号长度为零，设为最大EVM
    end
    
    % 显示结果
    fprintf('性能指标:\n');
    fprintf('误码率(BER): %.6f\n', ber);
    fprintf('误符号率(SER): %.6f\n', ser);
    fprintf('误差向量幅度(EVM): %.4f %%\n', evm*100);
end