function [rx_signal, channel_info] = underwater_channel(tx_signal, config)
    % 水声信道模拟
    % 包括多径、多普勒效应和环境噪声
    
    % 多径信道
    multipath_signal = multipath_channel(tx_signal, config.multipath_delays, config.multipath_gains);
    
    % 多普勒效应
    doppler_signal = doppler_effect(multipath_signal, config.doppler_shift, config.sampling_rate);
    
    % 环境噪声
    rx_signal = awgn_channel(doppler_signal, config.SNR, config.sound_speed, config.sampling_rate);
    
    % 返回信道信息
    channel_info.multipath_delays = config.multipath_delays;
    channel_info.multipath_gains = config.multipath_gains;
    channel_info.doppler_shift = config.doppler_shift;
    channel_info.SNR = config.SNR;
end

function output = multipath_channel(input, delays, gains)
    fprintf('多径信道处理 - 输入尺寸: %s\n', mat2str(size(input)));
    fprintf('延迟数组: %s, 增益数组: %s\n', mat2str(size(delays)), mat2str(size(gains)));
    
    % 确保输入是列向量
    if size(input, 2) > 1
        fprintf('转换输入为列向量\n');
        input = input(:);
    end
    
    output = zeros(size(input));
    fprintf('输出初始化尺寸: %s\n', mat2str(size(output)));
    
    for i = 1:length(delays)
        delay = delays(i);
        gain = gains(i);
        
        fprintf('处理第%d个多径: 延迟=%d, 增益=%.2f\n', i, delay, gain);
        
        if delay == 0
            % 直接路径
            delayed_signal = input;
        else
            % 延迟路径 - 确保不超过信号长度
            if delay >= length(input)
                error('多径延迟 %d 大于信号长度 %d', delay, length(input));
            end
            delayed_signal = [zeros(delay, 1); input(1:end-delay)];
        end
        
        fprintf('延迟信号尺寸: %s\n', mat2str(size(delayed_signal)));
        fprintf('当前输出尺寸: %s\n', mat2str(size(output)));
        
        % 确保尺寸匹配
        if ~isequal(size(delayed_signal), size(output))
            fprintf('尺寸不匹配，尝试调整...\n');
            % 如果维度不同但元素数相同，调整形状
            if numel(delayed_signal) == numel(output)
                delayed_signal = reshape(delayed_signal, size(output));
                fprintf('调整后延迟信号尺寸: %s\n', mat2str(size(delayed_signal)));
            else
                error('无法调整尺寸: 延迟信号%d元素, 输出%d元素',...
                    numel(delayed_signal), numel(output));
            end
        end
        
        % 执行加法
        output = output + gain * delayed_signal;
        fprintf('加法完成，新输出尺寸: %s\n', mat2str(size(output)));
    end
end

function output = doppler_effect(input, doppler_shift, fs)
    fprintf('多普勒效应 - 输入尺寸: %s\n', mat2str(size(input)));
    
    % 确保输入是列向量
    if size(input, 2) > 1
        input = input(:);
    end
    
    % 创建时间向量 - 确保与输入尺寸匹配
    t = (0:length(input)-1)' / fs;
    fprintf('时间向量尺寸: %s\n', mat2str(size(t)));
    
    if length(t) ~= length(input)
        error('时间向量长度%d与输入信号长度%d不匹配', length(t), length(input));
    end
    
    % 应用多普勒效应
    phase_shift = exp(1j*2*pi*doppler_shift*t);
    fprintf('相位偏移尺寸: %s\n', mat2str(size(phase_shift)));
    
    output = input .* phase_shift;
    fprintf('多普勒输出尺寸: %s\n', mat2str(size(output)));
end

function output = awgn_channel(input, snr_db, sound_speed, fs)
    % 水声环境噪声
    % 计算传输损失
    distance = 1000; % 假设传输距离1km
    TL = 20*log10(distance) + 0.1*distance; % 简化的传输损失模型
    
    % 计算接收信号功率
    signal_power = mean(abs(input).^2);
    
    % 环境噪声谱密度 (简化模型)
    noise_psd = 10^(-50/10); % 50dB re μPa/√Hz
    
    % 计算噪声功率
    noise_power = noise_psd * fs;
    
    % 计算接收SNR
    received_snr = 10^(snr_db/10) * 10^(-TL/10);
    
    % 添加噪声
    noise = sqrt(noise_power/received_snr) * (randn(size(input)) + 1j*randn(size(input)))/sqrt(2);
    output = input + noise;
end