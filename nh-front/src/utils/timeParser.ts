/**
 * 格式化时间（精确到分钟）
 * @param timeData 时间数组，格式为 [年, 月, 日, 时, 分]
 * @returns 格式化后的时间字符串
 */
export const formatTimeToMinute = (timeData?: number[]): string => {
    if (!timeData || timeData.length < 5) {
        return '未知时间'; // 确保至少有年、月、日、时、分数据
    }

    // 从数组中提取年、月、日、时、分（月份从0开始，需要减1）
    const [year, month, day, hour, minute] = timeData;

    // 手动补零处理，确保格式统一为 YYYY-MM-DD HH:MM
    const formattedYear = year.toString();
    const formattedMonth = month.toString().padStart(2, '0');
    const formattedDay = day.toString().padStart(2, '0');
    const formattedHour = hour.toString().padStart(2, '0');
    const formattedMinute = minute.toString().padStart(2, '0');

    return `${formattedYear}-${formattedMonth}-${formattedDay} ${formattedHour}:${formattedMinute}`;
};

/**
 * 格式化时间（仅精确到日）
 * @param timeData 时间数组，格式为 [年, 月, 日]
 * @returns 格式化后的时间字符串，格式为 YYYY-MM-DD
 */
export const formatTimeToDay = (timeData?: number[]): string => {
    if (!timeData || timeData.length < 3) {
        return '未知时间'; // 处理无效数据
    }

    // 从数组中提取年、月、日
    const [year, month, day] = timeData;

    // 手动补零处理，确保格式统一
    const formattedYear = year.toString();
    const formattedMonth = month.toString().padStart(2, '0');
    const formattedDay = day.toString().padStart(2, '0');

    return `${formattedYear}-${formattedMonth}-${formattedDay}`;
};

//时间精细处理
// 格式化时间
const formatTimeDetail = (date) => {
    // 首先确保date是有效的Date对象
    if (!(date instanceof Date) || isNaN(date.getTime())) {
        console.warn('无效的日期对象:', date);
        return '时间未知';
    }

    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMins / 60);
    //const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) {
        return '刚刚';
    } else if (diffMins < 60) {
        return `${diffMins}分钟前`;
    } else if (diffHours < 24) {
        return `${diffHours}小时前`;
    } else {
        // 使用Date对象的方法获取年月日时分
        const year = date.getFullYear();
        // 月份从0开始，需要加1
        const month = date.getMonth() + 1;
        const day = date.getDate();
        const hour = date.getHours();
        const minute = date.getMinutes();

        // 补零处理，确保格式统一为 YYYY-MM-DD HH:MM
        const formattedMonth = month.toString().padStart(2, '0');
        const formattedDay = day.toString().padStart(2, '0');
        const formattedHour = hour.toString().padStart(2, '0');
        const formattedMinute = minute.toString().padStart(2, '0');

        return `${year}-${formattedMonth}-${formattedDay} ${formattedHour}:${formattedMinute}`;
    }

    // if (diffMins < 1) {
    //   return '刚刚';
    // } else if (diffMins < 60) {
    //   return `${diffMins}分钟前`;
    // } else if (diffHours < 24) {
    //   return `${diffHours}小时前`;
    // } else if (diffDays < 30) {
    //   return `${diffDays}天前`;
    // } else {
    //   return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`;
    // }
};
