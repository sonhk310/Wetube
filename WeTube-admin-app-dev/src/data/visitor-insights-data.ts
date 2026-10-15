export const userInsightsData = {
  "normal users": Array.from({ length: 365 }, () =>
    Math.floor(Math.random() * 500)
  ),
  "pro users": Array.from({ length: 365 }, () =>
    Math.floor(Math.random() * 500)
  ),
};
