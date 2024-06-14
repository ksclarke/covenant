## NOID Details

Max java array size: 2,147,483,647
2 Gigabytes = 2,147,483,648 Bytes

| NOID Type              | 4 Char Length | 5 Char Length | 6 Char Length  | 7 Char Length     | 8 Char Length       |
| ---------------------- | ------------- | ------------- | -------------- | ----------------- | ------------------- |
| NUMERIC                | 10,000        | 100,000       | 1,000,000      | 10,000,000        | 100,000,000         |
| ALPHA                  | 390,625       | 9,765,625     | 244,140,625    | 6,103,515,625     | 152,587,890,625     |
| ALPHA_ALL              | 6,765,201     | 345,025,251   | 17,596,287,801 | 897,410,677,851   | 45,767,944,570,401  |
| ALPHANUMERIC           | 1,500,625     | 52,521,875    | 1,838,265,625  | 64,339,296,875    | 2,251,875,390,625   |
| ALPHANUMERIC_ALL       | 13,845,841    | 844,596,301   | 51,520,374,361 | 3,142,742,836,021 | 191,707,312,997,281 |

There are two additional NOID types not represented in the table above: REGEX_PATTERN and REGEX_PATTERN_ALL. The number of possible NOIDS created with each of these types will vary, depending on the regular expression patterns used, but they will be something below the values for ALPHANUMERIC and ALPHANUMERIC_ALL, which are the sets that the regular expressions are compared against.