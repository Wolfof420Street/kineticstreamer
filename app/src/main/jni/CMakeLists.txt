cmake_minimum_required(VERSION 3.22.1)

project(kinetic)

include(ExternalProject)

set(ANDROID_CMAKE_ARGS
        -DANDROID_ABI=${ANDROID_ABI}
        -DANDROID_PLATFORM=${ANDROID_PLATFORM}
        -DCMAKE_LIBRARY_OUTPUT_DIRECTORY=${CMAKE_LIBRARY_OUTPUT_DIRECTORY}
        -DCMAKE_BUILD_TYPE=${CMAKE_BUILD_TYPE}
        -DANDROID_NDK=${ANDROID_NDK}
        -DCMAKE_CXX_FLAGS=${CMAKE_CXX_FLAGS}
        -DCMAKE_TOOLCHAIN_FILE=${CMAKE_TOOLCHAIN_FILE}
        -DCMAKE_MAKE_PROGRAM=${CMAKE_MAKE_PROGRAM}
        -DCMAKE_INSTALL_PREFIX=${CMAKE_BINARY_DIR}
        -G${CMAKE_GENERATOR}
        -Wno-dev)

# Build external libraries
ExternalProject_Add(libjpeg-turbo
        GIT_REPOSITORY "https://github.com/libjpeg-turbo/libjpeg-turbo.git"
        GIT_TAG "3.0.2"
        CMAKE_ARGS ${ANDROID_CMAKE_ARGS} -DENABLE_SHARED=ON -DUSE_STATIC_MBEDTLS_LIBRARY=OFF
        -DUSE_SHARED_MBEDTLS_LIBRARY=ON
        BUILD_BYPRODUCTS ${CMAKE_BINARY_DIR}/lib/libjpeg.so ${CMAKE_BINARY_DIR}/lib/libturbojpeg.so
)

# After ExternalProject_Add for libjpeg-turbo:
add_library(jpeg SHARED IMPORTED)
add_library(turbojpeg SHARED IMPORTED)

ExternalProject_Add(libusb
        GIT_REPOSITORY "https://github.com/libusb/libusb-cmake.git"
        GIT_TAG "v1.0.27"
        CMAKE_ARGS ${ANDROID_CMAKE_ARGS} -DBUILD_SHARED_LIBS=ON
        BUILD_BYPRODUCTS ${CMAKE_BINARY_DIR}/lib/libusb-1.0.so
)

ExternalProject_Add(libuvc
        GIT_REPOSITORY "https://github.com/libuvc/libuvc.git"
        GIT_TAG "v0.0.7"
        CMAKE_ARGS ${ANDROID_CMAKE_ARGS}
                -DCMAKE_MODULE_PATH=${CMAKE_SOURCE_DIR}
                -DJPEG_LIBRARY=${CMAKE_BINARY_DIR}/lib/libjpeg.so
                -DJPEG_INCLUDE_DIR=${CMAKE_BINARY_DIR}/include
                -DLIBUSB_INCLUDE_DIR=${CMAKE_BINARY_DIR}/include/libusb-1.0
                -DLIBUSB_LIBRARY=${CMAKE_BINARY_DIR}/lib/libusb-1.0.so
        DEPENDS libjpeg-turbo libusb
        BUILD_BYPRODUCTS ${CMAKE_BINARY_DIR}/lib/libuvc.so
)

ExternalProject_Add(mbedtls
        GIT_REPOSITORY "https://github.com/Mbed-TLS/mbedtls.git"
        GIT_TAG "v3.6.3"
        CMAKE_ARGS ${ANDROID_CMAKE_ARGS}
                -DENABLE_TESTING=OFF
                -DENABLE_PROGRAMS=OFF
                -DUSE_STATIC_MBEDTLS_LIBRARY=OFF
                -DUSE_SHARED_MBEDTLS_LIBRARY=ON
        BUILD_BYPRODUCTS ${CMAKE_BINARY_DIR}/lib/libmbedtls.so ${CMAKE_BINARY_DIR}/lib/libmbedx509.so ${CMAKE_BINARY_DIR}/lib/libmbedcrypto.so
)

ExternalProject_Add(srt_external_build
        GIT_REPOSITORY "https://github.com/Haivision/srt.git"
        GIT_TAG "v1.5.4"
        CMAKE_ARGS ${ANDROID_CMAKE_ARGS}
            -DENABLE_APPS=OFF
            -DENABLE_LOGGING=ON
            -DENABLE_STDCXX_SYNC=ON
            -DENABLE_ENCRYPTION=ON
            -DUSE_ENCLIB=mbedtls
            -DMBEDTLS_INCLUDE_DIRS=${CMAKE_BINARY_DIR}/include
            -DMBEDTLS_INCLUDE_DIR=${CMAKE_BINARY_DIR}/include
            -DMBEDTLS_LIBRARIES=${CMAKE_BINARY_DIR}/lib/libmbedtls.so
            -DMBEDTLS_LIBRARIES=${CMAKE_BINARY_DIR}/lib/libmbedx509.so
            -DMBEDTLS_LIBRARIES=${CMAKE_BINARY_DIR}/lib/libmbedcrypto.so
        DEPENDS mbedtls
        BUILD_BYPRODUCTS ${CMAKE_BINARY_DIR}/lib/libsrt.so
)

# Add an imported target for srt
add_library(srt SHARED IMPORTED)
add_dependencies(srt srt_external_build) # Ensure srt_external_build is built before srt is used

# Copy libsrt.so to the go folder
add_custom_command(
        TARGET srt_external_build POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E make_directory ${CMAKE_SOURCE_DIR}/../go/third_party/srt/scripts/build-android/${ANDROID_ABI}/lib
        COMMAND ${CMAKE_COMMAND} -E make_directory ${CMAKE_SOURCE_DIR}/../go/third_party/srt/scripts/build-android/${ANDROID_ABI}/include/srt
        COMMAND ${CMAKE_COMMAND} -E copy
        ${CMAKE_BINARY_DIR}/lib/libsrt.so
        ${CMAKE_SOURCE_DIR}/../go/third_party/srt/scripts/build-android/${ANDROID_ABI}/lib/
        COMMAND ${CMAKE_COMMAND} -E copy_directory
        ${CMAKE_BINARY_DIR}/include/srt
        ${CMAKE_SOURCE_DIR}/../go/third_party/srt/scripts/build-android/${ANDROID_ABI}/include/srt
        COMMENT "Copying SRT libraries and headers to go/third_party/srt/scripts/build-android/${ANDROID_ABI}"
)

# Setup native library
link_directories(${CMAKE_BINARY_DIR}/lib)
include_directories(${CMAKE_BINARY_DIR}/include)

add_library(kinetic SHARED kinetic.c)
add_dependencies(kinetic libjpeg-turbo libusb libuvc mbedtls srt_external_build)
target_include_directories(kinetic PRIVATE ${CMAKE_BINARY_DIR}/include)

# Set properties for the imported libraries
set_target_properties(jpeg PROPERTIES
        IMPORTED_LOCATION "${CMAKE_BINARY_DIR}/lib/libjpeg.so"
        IMPORTED_SONAME "libjpeg.so")

set_target_properties(turbojpeg PROPERTIES
        IMPORTED_LOCATION "${CMAKE_BINARY_DIR}/lib/libturbojpeg.so"
        IMPORTED_SONAME "libturbojpeg.so")

set_target_properties(srt PROPERTIES
        IMPORTED_LOCATION "${CMAKE_BINARY_DIR}/lib/libsrt.so"
        IMPORTED_SONAME "libsrt.so")

# Link directly against the libraries
target_link_libraries(kinetic
        jpeg
        turbojpeg
        ${CMAKE_BINARY_DIR}/lib/libusb-1.0.so
        ${CMAKE_BINARY_DIR}/lib/libuvc.so
        ${CMAKE_BINARY_DIR}/lib/libmbedtls.so
        ${CMAKE_BINARY_DIR}/lib/libmbedx509.so
        ${CMAKE_BINARY_DIR}/lib/libmbedcrypto.so
        srt
)