function [equalized_signal, weights, error] = rls_equalizer(rx_signal, training_seq, forgetting_factor, filter_length)
    % RLS自适应均衡器
    % 输入:
    %   rx_signal - 接收信号
    %   training_seq - 训练序列
    %   forgetting_factor - 遗忘因子(0 < λ ≤ 1)
    %   filter_length - 均衡器长度
    % 输出:
    %   equalized_signal - 均衡后信号
    %   weights - 最终权重
    %   error - 误差序列
    
    % 初始化
    signal_length = length(rx_signal);
    training_length = length(training_seq);
    weights = zeros(filter_length, 1);
    P = eye(filter_length) / 0.01; % 初始逆相关矩阵
    equalized_signal = zeros(signal_length, 1);
    error = zeros(signal_length, 1);
    
    % RLS算法
    for n = filter_length:signal_length
        % 获取当前输入向量
        x = rx_signal(n:-1:n-filter_length+1);
        
        % 先验估计
        y = weights' * x;
        equalized_signal(n) = y;
        
        % 计算先验误差
        if n <= training_length
            e = training_seq(n) - y;
        else
            % 决策导向模式
            decision = qamdemod(y, 16, 'OutputType', 'integer');
            e = qammod(decision, 16, 'UnitAveragePower', true) - y;
        end
        error(n) = abs(e)^2;
        
        % 计算增益向量
        k = (P * x) / (forgetting_factor + x' * P * x);
        
        % 更新权重
        weights = weights + k * conj(e);
        
        % 更新逆相关矩阵
        P = (P - k * x' * P) / forgetting_factor;
    end
end