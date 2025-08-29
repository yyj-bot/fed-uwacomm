function windowing()
    % 窗函数工具主函数 - 包含各种窗函数实现
    % 用于OFDM符号的加窗处理以减少频谱泄漏
end

%% 升余弦窗
function window = rcos_window(alpha, length, varargin)
    % 生成升余弦窗
    % 输入:
    %   alpha - 滚降系数 (0-1)
    %   length - 窗长度
    %   varargin - 可选参数: 'type'
    
    p = inputParser;
    addParameter(p, 'type', 'normal', @ischar); % 'normal' or 'sqrt'
    parse(p, varargin{:});
    
    if alpha < 0 || alpha > 1
        error('滚降系数必须在0到1之间');
    end
    
    window = zeros(length, 1);
    n = (0:length-1)';
    
    switch p.Results.type
        case 'normal'
            % 标准升余弦窗
            T = length / (2 * (1 + alpha));
            for i = 1:length
                if n(i) <= (1 - alpha) * T
                    window(i) = 1;
                elseif n(i) <= (1 + alpha) * T
                    window(i) = 0.5 * (1 + cos(pi/alpha * (n(i)/T - 1)));
                else
                    window(i) = 0;
                end
            end
            
        case 'sqrt'
            % 平方根升余弦窗
            T = length / 2;
            for i = 1:length
                if n(i) <= (1 - alpha) * T
                    window(i) = 1;
                elseif n(i) <= (1 + alpha) * T
                    window(i) = sqrt(0.5 * (1 + cos(pi/alpha * (n(i)/T - 1))));
                else
                    window(i) = 0;
                end
            end
            
        otherwise
            error('未知的窗类型');
    end
end

%% 矩形窗
function window = rect_window(length)
    % 生成矩形窗
    window = ones(length, 1);
end

%% 汉宁窗
function window = hann_window(length)
    % 生成汉宁窗
    n = (0:length-1)';
    window = 0.5 * (1 - cos(2*pi*n/(length-1)));
end

%% 汉明窗
function window = hamming_window(length)
    % 生成汉明窗
    n = (0:length-1)';
    window = 0.54 - 0.46 * cos(2*pi*n/(length-1));
end

%% 布莱克曼窗
function window = blackman_window(length)
    % 生成布莱克曼窗
    n = (0:length-1)';
    window = 0.42 - 0.5*cos(2*pi*n/(length-1)) + 0.08*cos(4*pi*n/(length-1));
end

%% 窗函数比较
function compare_windows(length, varargin)
    % 比较不同窗函数的性能
    % 输入:
    %   length - 窗长度
    %   varargin - 要比较的窗函数列表
    
    p = inputParser;
    addParameter(p, 'windows', {'rect', 'hann', 'hamming', 'blackman', 'rcos'}, @iscell);
    addParameter(p, 'alpha', 0.2, @isnumeric);
    parse(p, varargin{:});
    
    figure('Name', '窗函数比较', 'Position', [100, 100, 1000, 800]);
    
    % 时域比较
    subplot(2,2,1);
    hold on;
    colors = lines(length(p.Results.windows));
    for i = 1:length(p.Results.windows)
        switch p.Results.windows{i}
            case 'rect'
                w = rect_window(length);
            case 'hann'
                w = hann_window(length);
            case 'hamming'
                w = hamming_window(length);
            case 'blackman'
                w = blackman_window(length);
            case 'rcos'
                w = rcos_window(p.Results.alpha, length);
        end
        plot(w, 'LineWidth', 2, 'Color', colors(i,:));
    end
    hold off;
    grid on;
    title('时域窗函数');
    xlabel('采样点');
    ylabel('幅度');
    legend(p.Results.windows, 'Location', 'best');
    
    % 频域比较
    subplot(2,2,2);
    hold on;
    for i = 1:length(p.Results.windows)
        switch p.Results.windows{i}
            case 'rect'
                w = rect_window(length);
            case 'hann'
                w = hann_window(length);
            case 'hamming'
                w = hamming_window(length);
            case 'blackman'
                w = blackman_window(length);
            case 'rcos'
                w = rcos_window(p.Results.alpha, length);
        end
        [H, f] = freqz(w, 1, 1024, 'whole');
        plot(f/(2*pi), 20*log10(abs(H)), 'LineWidth', 2, 'Color', colors(i,:));
    end
    hold off;
    grid on;
    title('频域响应');
    xlabel('归一化频率 (×π rad/sample)');
    ylabel('幅度 (dB)');
    ylim([-100, 0]);
    
    % 主瓣宽度比较
    subplot(2,2,3);
    mainlobe_width = zeros(length(p.Results.windows), 1);
    for i = 1:length(p.Results.windows)
        switch p.Results.windows{i}
            case 'rect'
                w = rect_window(length);
            case 'hann'
                w = hann_window(length);
            case 'hamming'
                w = hamming_window(length);
            case 'blackman'
                w = blackman_window(length);
            case 'rcos'
                w = rcos_window(p.Results.alpha, length);
        end
        [H, f] = freqz(w, 1, 1024, 'whole');
        H_db = 20*log10(abs(H));
        mainlobe_width(i) = calculate_mainlobe_width(f, H_db);
    end
    bar(mainlobe_width);
    set(gca, 'XTickLabel', p.Results.windows);
    grid on;
    title('主瓣宽度比较');
    ylabel('主瓣宽度 (rad/sample)');
    
    % 旁瓣衰减比较
    subplot(2,2,4);
    sidelobe_atten = zeros(length(p.Results.windows), 1);
    for i = 1:length(p.Results.windows)
        switch p.Results.windows{i}
            case 'rect'
                w = rect_window(length);
            case 'hann'
                w = hann_window(length);
            case 'hamming'
                w = hamming_window(length);
            case 'blackman'
                w = blackman_window(length);
            case 'rcos'
                w = rcos_window(p.Results.alpha, length);
        end
        [H, f] = freqz(w, 1, 1024, 'whole');
        H_db = 20*log10(abs(H));
        sidelobe_atten(i) = calculate_sidelobe_attenuation(H_db);
    end
    bar(sidelobe_atten);
    set(gca, 'XTickLabel', p.Results.windows);
    grid on;
    title('最大旁瓣衰减比较');
    ylabel('衰减 (dB)');
end

%% OFDM加窗处理
function windowed_symbols = apply_ofdm_windowing(symbols, window_type, varargin)
    % 对OFDM符号应用窗函数
    % 输入:
    %   symbols - OFDM符号矩阵
    %   window_type - 窗类型
    %   varargin - 窗参数
    
    p = inputParser;
    addParameter(p, 'alpha', 0.2, @isnumeric);
    addParameter(p, 'overlap', 0, @isnumeric);
    parse(p, varargin{:});
    
    [symbol_length, num_symbols] = size(symbols);
    
    switch window_type
        case 'rcos'
            window = rcos_window(p.Results.alpha, symbol_length);
        case 'hann'
            window = hann_window(symbol_length);
        case 'hamming'
            window = hamming_window(symbol_length);
        case 'blackman'
            window = blackman_window(symbol_length);
        case 'rect'
            window = rect_window(symbol_length);
        otherwise
            error('不支持的窗类型');
    end
    
    % 应用窗函数
    windowed_symbols = symbols .* repmat(window, 1, num_symbols);
    
    % 处理重叠部分（如果指定）
    if p.Results.overlap > 0
        windowed_symbols = apply_overlap(windowed_symbols, p.Results.overlap);
    end
end

%% 辅助函数
function width = calculate_mainlobe_width(f, H_db)
    % 计算主瓣宽度
    [max_val, max_idx] = max(H_db);
    threshold = max_val - 3; % 3dB带宽
    
    % 找到左边界
    left_idx = max_idx;
    while left_idx > 1 && H_db(left_idx) > threshold
        left_idx = left_idx - 1;
    end
    
    % 找到右边界
    right_idx = max_idx;
    while right_idx < length(H_db) && H_db(right_idx) > threshold
        right_idx = right_idx + 1;
    end
    
    width = f(right_idx) - f(left_idx);
end

function attenuation = calculate_sidelobe_attenuation(H_db)
    % 计算最大旁瓣衰减
    [max_val, max_idx] = max(H_db);
    
    % 排除主瓣区域（主瓣宽度约为4π/N）
    exclude_region = round(0.1 * length(H_db));
    left_exclude = max(1, max_idx - exclude_region);
    right_exclude = min(length(H_db), max_idx + exclude_region);
    
    % 找到最大旁瓣
    sidelobe_db = H_db;
    sidelobe_db(left_exclude:right_exclude) = -Inf;
    max_sidelobe = max(sidelobe_db);
    
    attenuation = max_val - max_sidelobe;
end

function overlapped = apply_overlap(symbols, overlap_samples)
    % 应用重叠相加
    [symbol_length, num_symbols] = size(symbols);
    total_length = symbol_length * num_symbols - overlap_samples * (num_symbols - 1);
    
    overlapped = zeros(total_length, 1);
    
    for i = 1:num_symbols
        start_idx = (i-1) * (symbol_length - overlap_samples) + 1;
        end_idx = start_idx + symbol_length - 1;
        
        if i == 1
            overlapped(start_idx:end_idx) = symbols(:, i);
        else
            % 重叠部分相加
            overlap_region = start_idx:start_idx+overlap_samples-1;
            overlapped(overlap_region) = overlapped(overlap_region) + symbols(1:overlap_samples, i);
            overlapped(start_idx+overlap_samples:end_idx) = symbols(overlap_samples+1:end, i);
        end
    end
end

%% 窗函数性能分析
function analyze_window_performance(signal, window_type, varargin)
    % 分析窗函数对信号性能的影响
    % 输入:
    %   signal - 输入信号
    %   window_type - 窗类型
    %   varargin - 窗参数
    
    p = inputParser;
    addParameter(p, 'alpha', 0.2, @isnumeric);
    addParameter(p, 'fs', 1, @isnumeric);
    parse(p, varargin{:});
    
    % 应用窗函数
    switch window_type
        case 'rcos'
            window = rcos_window(p.Results.alpha, length(signal));
        case 'hann'
            window = hann_window(length(signal));
        case 'hamming'
            window = hamming_window(length(signal));
        case 'blackman'
            window = blackman_window(length(signal));
        case 'rect'
            window = rect_window(length(signal));
    end
    
    windowed_signal = signal .* window;
    
    % 计算性能指标
    original_power = mean(abs(signal).^2);
    windowed_power = mean(abs(windowed_signal).^2);
    power_loss = 10*log10(windowed_power/original_power);
    
    % 计算频谱泄漏
    original_spectrum = abs(fft(signal));
    windowed_spectrum = abs(fft(windowed_signal));
    
    % 找到主瓣能量
    [~, mainlobe_idx] = max(original_spectrum);
    mainlobe_region = max(1, mainlobe_idx-10):min(length(signal), mainlobe_idx+10);
    
    original_main_energy = sum(original_spectrum(mainlobe_region).^2);
    windowed_main_energy = sum(windowed_spectrum(mainlobe_region).^2);
    
    original_total_energy = sum(original_spectrum.^2);
    windowed_total_energy = sum(windowed_spectrum.^2);
    
    leakage_reduction = 10*log10((original_total_energy-original_main_energy)/(windowed_total_energy-windowed_main_energy));
    
    fprintf('窗函数性能分析 (%s):\n', window_type);
    fprintf('功率损失: %.2f dB\n', power_loss);
    fprintf('频谱泄漏改善: %.2f dB\n', leakage_reduction);
end