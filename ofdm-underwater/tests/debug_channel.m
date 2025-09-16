function debug_channel()
    % 单独测试水声信道函数
    
    init_project;
    
    % 使用完整的配置函数而不是手动创建
    config = underwater_config();
    
    % 创建简单测试信号
    test_signal = (1:1000)' + 1j*(1000:-1:1)';
    fprintf('测试信号尺寸: %s\n', mat2str(size(test_signal)));
    
    fprintf('开始测试水声信道...\n');
    fprintf('多径延迟: %s\n', mat2str(config.multipath_delays));
    fprintf('多径增益: %s\n', mat2str(config.multipath_gains));
    fprintf('声速: %d m/s\n', config.sound_speed);
    
    try
        [output, info] = underwater_channel(test_signal, config);
        fprintf('测试成功! 输出信号尺寸: %s\n', mat2str(size(output)));
    catch ME
        fprintf('测试失败: %s\n', ME.message);
        fprintf('错误位置: %s, 行号: %d\n', ME.stack(1).name, ME.stack(1).line);
    end
end