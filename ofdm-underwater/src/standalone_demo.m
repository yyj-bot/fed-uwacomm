function standalone_demo()
% STANDALONE_DEMO - 独立运行演示函数

    fprintf('=== OFDM水声通信系统独立演示 ===\n');
    
    % 确保项目初始化
    init_project();
    
    % 使用默认配置
    config = underwater_config();
    
    % 设置演示参数
    config.SNR_range = 0:5:20;
    config.plot_results = false;
    config.save_results = true;
    
    % 运行主系统
    [results, image_paths] = underwater_ofdm_system_main(config);
    
    % 显示结果
    fprintf('\n=== 仿真结果 ===\n');
    fprintf('SNR范围: %s dB\n', mat2str(config.SNR_range));
    fprintf('最小BER: %.6f\n', min(results.ber));
    fprintf('最大BER: %.6f\n', max(results.ber));
    fprintf('生成图片数量: %d\n', length(image_paths));
    
    % 显示保存位置
    if ~isempty(image_paths)
        [output_dir, ~, ~] = fileparts(image_paths{1});
        [output_parent, ~, ~] = fileparts(output_dir);
        fprintf('\n文件保存位置:\n');
        fprintf('输出目录: %s\n', output_parent);
        fprintf('图片文件夹: %s\n', output_dir);
        fprintf('数据文件夹: %s/data\n', output_parent);
        
        % 询问是否打开文件夹
        choice = input('\n是否打开输出文件夹? (y/n): ', 's');
        if strcmpi(choice, 'y') || strcmpi(choice, 'yes')
            winopen(output_parent);  % Windows
            % system(['open ', output_parent]);  % Mac
            % system(['nautilus ', output_parent]);  % Linux
        end
    end
end