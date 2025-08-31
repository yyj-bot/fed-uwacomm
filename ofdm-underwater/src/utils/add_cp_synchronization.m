function synced_signal = add_cp_synchronization(rx_signal, cp_length, ifft_length)
    % 基于循环前缀的同步
    
    symbol_length = ifft_length + cp_length;
    search_range = min(1000, length(rx_signal) - symbol_length);
    
    correlations = zeros(search_range, 1);
    
    for n = 1:search_range
        % 提取可能的符号
        symbol = rx_signal(n:n+symbol_length-1);
        
        % 计算CP与对应数据的相关性
        cp_part = symbol(1:cp_length);
        data_part = symbol(ifft_length+1:ifft_length+cp_length);
        
        correlations(n) = abs(cp_part' * data_part) / (norm(cp_part) * norm(data_part));
    end
    
    [~, sync_pos] = max(correlations);
    synced_signal = rx_signal(sync_pos:end);
    
    fprintf('同步位置: %d, 最大相关性: %.4f\n', sync_pos, max(correlations));
end