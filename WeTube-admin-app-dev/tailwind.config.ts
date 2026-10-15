/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: "class",
  content: ["./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      animation: {
        "gradient-x": "gradient-x 8s ease infinite",
      },
      keyframes: {
        "gradient-x": {
          "0%, 100%": {
            "background-position": "0% 50%",
          },
          "50%": {
            "background-position": "100% 50%",
          },
        },
      },
      backgroundSize: {
        "400": "400% 400%",
      },

      colors: {
        dark1: "#21244d",
        dark2: "#4a26ab",
        dark3: "#9543a7",
        dark4: "#e350a8",
        dark5: "#b8e7ea",

        light1: "#436c5bff",
        light2: "#A4C3A2",
        light3: "#D0D4B8",
        light4: "#EAE7D6",
        light5: "#D7F9FA",
      },
    },
  },
  plugins: [],
};
