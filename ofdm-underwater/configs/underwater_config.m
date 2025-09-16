function config = underwater_config()
    % OFDM系统参数
    config.carrier_count = 200;       % 子载波数
    config.symbol_count = 100;        % 总符号数
    config.ifft_length = 512;         % IFFT长度
    config.CP_length = 128;           % 循环前缀长度
    config.CS_length = 20;            % 循环后缀长度
    config.bit_per_symbol = 4;        % 每符号比特数 (16QAM)
    config.alpha = 1.5/32;            % 升余弦窗系数
    
    % 水声信道参数
    config.sampling_rate = 50000;      % 采样率 (Hz)
    config.sound_speed = 1500;        % 声速 (m/s)
    config.doppler_shift = 10;        % 多普勒频移 (Hz)
    config.multipath_delays = [0, 20, 50]; % 多径时延 (采样点)
    config.multipath_gains = [1, 0.2, 0.1]; % 多径增益
    
     if length(config.multipath_delays) ~= length(config.multipath_gains)
        error('multipath_delays和multipath_gains数组长度必须相同');
     end
     
    % 自适应均衡参数
    config.equalizer_type = 'LMS';    % LMS/RLS/MMSE
    config.equalizer_length = 32;     % 均衡器长度
    config.step_size = 0.01;          % LMS步长
    config.forgetting_factor = 0.98;  % RLS遗忘因子
    config.enable_time_domain_equalization = false;
    config.enable_freq_domain_equalization = true;

    % 仿真参数
    config.SNR_range = 0:5:30;       % SNR范围 (dB)
    config.plot_results = true;       % 是否绘制结果
    config.save_results = false;      % 是否保存结果

    config.enable_doppler_compensation = false;
    config.use_enhanced_channel = false;
    config.enhanced_channel_estimation = false;

    fprintf('配置加载成功\n');
end