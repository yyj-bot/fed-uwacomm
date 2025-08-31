function [equalized_signal, weights, error] = lms_equalizer(rx_signal, training_seq, step_size, filter_length)
    % LMS自适应均衡器
    % 输入:
    %   rx_signal - 接收信号
    %   training_seq - 训练序列
    %   step_size - LMS步长
    %   filter_length - 均衡器长度
    % 输出:
    %   equalized_signal - 均衡后信号
    %   weights - 最终权重
    %   error - 误差序列
    
    % 初始化
    signal_length = length(rx_signal);
    training_length = length(training_seq);
    weights = zeros(filter_length, 1);
    equalized_signal = zeros(signal_length, 1);
    error = zeros(signal_length, 1);
    
    % 训练阶段
    for n = filter_length:training_length
        x = rx_signal(n:-1:n-filter_length+1);
        y = weights' * x;
        e = training_seq(n) - y;
        weights = weights + step_size * conj(e) * x;
        equalized_signal(n) = y;
        error(n) = abs(e)^2;
    end
    
    % 跟踪阶段 (决策导向)
    for n = training_length+1:signal_length
        x = rx_signal(n:-1:n-filter_length+1);
        y = weights' * x;
        decision = qamdemod(y, 16, 'OutputType', 'integer');
        e = qammod(decision, 16, 'UnitAveragePower', true) - y;
        weights = weights + step_size * conj(e) * x;
        equalized_signal(n) = y;
        error(n) = abs(e)^2;
    end
end