package com.example.wordgame.data

// The API returns a list of strings, e.g., ["word"]
// So, we expect a List<String>
// For simplicity, we can also just expect a single string if the API always returns one word in an array.
// However, to be robust, let's treat it as a list and take the first element.
// typealias WordResponse = List<String>
// Simpler: If the API is guaranteed to return ["word"], we can deserialize it as a list
// and then extract the first element.

// Let's assume the API structure is literally just an array of strings like:
// [ "theword" ]
// Retrofit can directly deserialize this into a List<String>