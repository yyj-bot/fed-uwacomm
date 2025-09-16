function plotting()
    % 绘图工具主函数 - 包含所有绘图相关的功能
    % 这个文件提供各种可视化功能用于OFDM系统分析
end

%% 信号时域可视化
function plot_time_domain(signals, varargin)
    % 绘制时域信号
    % 输入:
    %   signals - 信号结构体或矩阵
    %   varargin - 可选参数: 'title', 'xlabel', 'ylabel', 'legend'
    
    p = inputParser;
    addParameter(p, 'title', '时域信号', @ischar);
    addParameter(p, 'xlabel', '采样点', @ischar);
    addParameter(p, 'ylabel', '幅度', @ischar);
    addParameter(p, 'legend', {}, @iscell);
    addParameter(p, 'samples', 1000, @isnumeric);
    parse(p, varargin{:});
    
    figure('Name', '时域信号分析', 'Position', [100, 100, 800, 600]);
    
    if isstruct(signals)
        % 处理结构体输入
        field_names = fieldnames(signals);
        num_signals = length(field_names);
        
        for i = 1:num_signals
            signal = signals.(field_names{i});
            plot_data = real(signal(1:min(p.Results.samples, length(signal))));
            
            subplot(num_signals, 1, i);
            plot(plot_data, 'LineWidth', 1.5);
            title([field_names{i} ' - 实部']);
            xlabel(p.Results.xlabel);
            ylabel(p.Results.ylabel);
            grid on;
        end
        
    else
        % 处理矩阵输入
        if size(signals, 2) > 1
            for i = 1:size(signals, 2)
                subplot(size(signals, 2), 1, i);
                plot(real(signals(1:min(p.Results.samples, length(signals)), i)), 'LineWidth', 1.5);
                if ~isempty(p.Results.legend) && length(p.Results.legend) >= i
                    title(p.Results.legend{i});
                end
                xlabel(p.Results.xlabel);
                ylabel(p.Results.ylabel);
                grid on;
            end
        else
            plot(real(signals(1:min(p.Results.samples, length(signals)))), 'LineWidth', 1.5);
            title(p.Results.title);
            xlabel(p.Results.xlabel);
            ylabel(p.Results.ylabel);
            grid on;
        end
    end
end

%% 信号频域可视化
function plot_frequency_domain(signals, fs, varargin)
    % 绘制频域信号
    % 输入:
    %   signals - 信号
    %   fs - 采样率
    %   varargin - 可选参数
    
    p = inputParser;
    addParameter(p, 'title', '频域信号', @ischar);
    addParameter(p, 'xlabel', '频率 (Hz)', @ischar);
    addParameter(p, 'ylabel', '幅度 (dB)', @ischar);
    addParameter(p, 'normalize', true, @islogical);
    parse(p, varargin{:});
    
    figure('Name', '频域分析', 'Position', [100, 100, 800, 400]);
    
    if iscell(signals)
        colors = lines(length(signals));
        hold on;
        for i = 1:length(signals)
            signal = signals{i};
            N = length(signal);
            f = (-fs/2:fs/N:fs/2-fs/N);
            spectrum = fftshift(20*log10(abs(fft(signal)/N)));
            
            if p.Results.normalize
                spectrum = spectrum - max(spectrum);
            end
            
            plot(f/1e3, spectrum, 'LineWidth', 1.5, 'Color', colors(i,:));
        end
        hold off;
        
    else
        N = length(signals);
        f = (-fs/2:fs/N:fs/2-fs/N);
        spectrum = fftshift(20*log10(abs(fft(signals)/N)));
        
        if p.Results.normalize
            spectrum = spectrum - max(spectrum);
        end
        
        plot(f/1e3, spectrum, 'LineWidth', 1.5);
    end
    
    title(p.Results.title);
    xlabel(p.Results.xlabel);
    ylabel(p.Results.ylabel);
    grid on;
    legend('show');
end

%% 星座图绘制
function plot_constellation(symbols, varargin)
    % 绘制星座图
    % 输入:
    %   symbols - 调制符号
    %   varargin - 可选参数
    
    p = inputParser;
    addParameter(p, 'title', '星座图', @ischar);
    addParameter(p, 'modulation', 'QAM', @ischar);
    addParameter(p, 'order', 16, @isnumeric);
    addParameter(p, 'reference', false, @islogical);
    parse(p, varargin{:});
    
    figure('Name', '星座图分析', 'Position', [100, 100, 600, 500]);
    
    scatter(real(symbols), imag(symbols), 30, 'filled', 'MarkerFaceAlpha', 0.6);
    axis square;
    grid on;
    
    title(p.Results.title);
    xlabel('同相分量 (I)');
    ylabel('正交分量 (Q)');
    
    % 绘制参考星座点
    if p.Results.reference
        hold on;
        ref_symbols = qammod(0:p.Results.order-1, p.Results.order, 'UnitAveragePower', true);
        scatter(real(ref_symbols), imag(ref_symbols), 100, 'rx', 'LineWidth', 2);
        hold off;
        legend('接收符号', '参考星座点');
    end
    
    % 计算并显示EVM
    if p.Results.reference
        evm = calculate_evm(symbols, ref_symbols);
        text(0.05, 0.95, sprintf('EVM: %.2f%%', evm*100), ...
            'Units', 'normalized', 'BackgroundColor', 'white');
    end
end

%% BER曲线绘制
function plot_ber_curve(snr_range, ber_results, varargin)
    % 绘制BER曲线
    % 输入:
    %   snr_range - SNR范围(dB)
    %   ber_results - BER结果
    
    p = inputParser;
    addParameter(p, 'title', 'BER性能曲线', @ischar);
    addParameter(p, 'xlabel', 'SNR (dB)', @ischar);
    addParameter(p, 'ylabel', '误码率 (BER)', @ischar);
    addParameter(p, 'legend', {}, @iscell);
    addParameter(p, 'theory', false, @islogical);
    parse(p, varargin{:});
    
    figure('Name', 'BER性能分析', 'Position', [100, 100, 700, 500]);
    
    if iscell(ber_results)
        colors = lines(length(ber_results));
        hold on;
        for i = 1:length(ber_results)
            semilogy(snr_range, ber_results{i}, 'o-', 'LineWidth', 2, ...
                'Color', colors(i,:), 'MarkerSize', 6);
        end
        hold off;
    else
        semilogy(snr_range, ber_results, 'bo-', 'LineWidth', 2, 'MarkerSize', 8);
    end
    
    if p.Results.theory
        hold on;
        % 绘制理论BER曲线 (AWGN信道，16QAM)
        snr_linear = 10.^(snr_range/10);
        ber_theory = 3/4 * erfc(sqrt(snr_linear/10));
        semilogy(snr_range, ber_theory, 'r--', 'LineWidth', 2);
        hold off;
    end
    
    grid on;
    title(p.Results.title);
    xlabel(p.Results.xlabel);
    ylabel(p.Results.ylabel);
    
    if ~isempty(p.Results.legend)
        legend(p.Results.legend, 'Location', 'southwest');
    elseif p.Results.theory
        legend('仿真结果', '理论值(AWGN)');
    end
    
    ylim([1e-6, 1]);
end

%% 均衡器收敛曲线
function plot_equalizer_convergence(error, varargin)
    % 绘制均衡器收敛曲线
    % 输入:
    %   error - 误差序列
    
    p = inputParser;
    addParameter(p, 'title', '均衡器收敛曲线', @ischar);
    addParameter(p, 'xlabel', '迭代次数', @ischar);
    addParameter(p, 'ylabel', 'MSE (dB)', @ischar);
    parse(p, varargin{:});
    
    figure('Name', '均衡器性能分析', 'Position', [100, 100, 700, 400]);
    
    mse_db = 10*log10(error(error > 0));
    plot(mse_db, 'LineWidth', 2);
    
    grid on;
    title(p.Results.title);
    xlabel(p.Results.xlabel);
    ylabel(p.Results.ylabel);
end

%% 多径信道响应可视化
function plot_multipath_response(delays, gains, varargin)
    % 绘制多径信道响应
    % 输入:
    %   delays - 时延数组
    %   gains - 增益数组
    
    p = inputParser;
    addParameter(p, 'title', '多径信道响应', @ischar);
    addParameter(p, 'xlabel', '时延 (采样点)', @ischar);
    addParameter(p, 'ylabel', '增益', @ischar);
    parse(p, varargin{:});
    
    figure('Name', '信道特性分析', 'Position', [100, 100, 700, 400]);
    
    stem(delays, gains, 'filled', 'LineWidth', 2, 'MarkerSize', 8);
    
    grid on;
    title(p.Results.title);
    xlabel(p.Results.xlabel);
    ylabel(p.Results.ylabel);
end

%% 辅助函数
function evm = calculate_evm(received, reference)
    % 计算EVM
    error = received - reference;
    evm = sqrt(mean(abs(error).^2)) / sqrt(mean(abs(reference).^2));
end