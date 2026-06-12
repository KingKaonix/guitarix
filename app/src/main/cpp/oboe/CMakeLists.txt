# Oboe - static library for low-latency audio on Android
cmake_minimum_required(VERSION 3.10)

project(oboe C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

file(GLOB_RECURSE oboe_SRC
    "src/*.cpp"
)

add_library(oboe STATIC ${oboe_SRC})

target_include_directories(oboe PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_link_libraries(oboe
    log
)

# Suppress noisy warnings
target_compile_options(oboe PRIVATE
    -w
)
