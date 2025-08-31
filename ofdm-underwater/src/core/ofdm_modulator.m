function [tx_signal, params] = ofdm_modulator(bit_sequence, config)
    % OFDM调制器
    % 输入:
    %   bit_sequence - 输入比特序列
    %   config - 配置参数
    % 输出:
    %   tx_signal - 时域OFDM信号
    %   params - 调制参数结构体
    
    % 参数提取
    carrier_count = config.carrier_count;
    symbol_count = config.symbol_count;
    ifft_length = config.ifft_length;
    CP_length = config.CP_length;
    CS_length = config.CS_length;
    bit_per_symbol = config.bit_per_symbol;
    alpha = config.alpha;
    
    % 验证输入比特长度
    bit_length = carrier_count * symbol_count * bit_per_symbol;
    if length(bit_sequence) ~= bit_length
        error('输入比特序列长度不匹配配置参数');
    end
    
    % 16QAM调制
    modulated_symbols = qammod(bit_sequence, 16, 'InputType', 'bit', 'UnitAveragePower', true);
    
    % 子载波映射 (Hermitian对称)
    carrier_position = 29:228;
    conj_position = 485:-1:286;
    ifft_input = zeros(ifft_length, symbol_count);
    
    % 重塑为符号矩阵
    modulated_symbols = reshape(modulated_symbols, carrier_count, symbol_count);
    
    % 子载波映射
    ifft_input(carrier_position, :) = modulated_symbols;
    ifft_input(conj_position, :) = conj(modulated_symbols);
    
    % IFFT变换
    time_domain_symbols = ifft(ifft_input, ifft_length);
    
    % 添加循环前缀和后缀
    cp_symbols = [time_domain_symbols(end-CP_length+1:end, :); time_domain_symbols];
    cpc_symbols = [cp_symbols; cp_symbols(1:CS_length, :)];
    
    % 加窗处理
    window = rcos_window(alpha, size(cpc_symbols, 1));
    windowed_symbols = cpc_symbols .* repmat(window, 1, symbol_count);
    
    % 并串转换
    tx_signal = reshape(windowed_symbols, [], 1);
    
    % 保存参数用于解调
    params.carrier_position = carrier_position;
    params.conj_position = conj_position;
    params.modulation_order = 16;
    params.ifft_length = ifft_length;
    params.CP_length = CP_length;
    params.CS_length = CS_length;
end

function window = rcos_window(alpha, length)
    % 升余弦窗函数
    window = zeros(length, 1);
    T = length / (2 * (1 + alpha));
    for n = 1:length
        if n <= (1 - alpha) * T
            window(n) = 1;
        elseif n <= (1 + alpha) * T
            window(n) = 0.5 * (1 - sin(pi / (2 * alpha * T) * (n - T)));
        else
            window(n) = 0;
        end
    end
end