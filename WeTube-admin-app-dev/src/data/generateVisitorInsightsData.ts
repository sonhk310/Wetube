export function generateVisitorInsightsData(mode: 'day' | 'month' | 'year') {
  const counts = {
    day: 30, // 30 ngày
    month: 12, // 12 tháng
    year: 5, // 5 năm
  };

  const length = counts[mode];

  return {
    'normal users': Array.from({ length }, () => Math.floor(Math.random() * 500)),
    'pro users': Array.from({ length }, () => Math.floor(Math.random() * 500)),
  };
}
