import reactPlugin from "eslint-plugin-react";

export default [
  {
    plugins: {
      react: reactPlugin,
    },
    rules: {
      "react/no-unknown-property": [
        "error",
        {
          ignore: [
            "geometry",
            "material",
            "args",
            "position",
            "rotation",
            "scale",
          ],
        },
      ],
    },
  },
];
