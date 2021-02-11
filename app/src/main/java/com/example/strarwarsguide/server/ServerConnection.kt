package com.example.strarwarsguide.server

import com.apollographql.apollo.ApolloClient

val apolloClient: ApolloClient = ApolloClient.builder()
    .serverUrl("https://swapi-graphql.netlify.app/.netlify/functions/index")
    .build()