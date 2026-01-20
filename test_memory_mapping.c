/**
 * @file test_memory_mapping.c
 * @brief Test and demonstration of CheerpJ buffer mapping utilities
 * 
 * This file demonstrates how to use the memory mapping utilities to
 * interact with CheerpJ ByteBuffer objects from C/WebAssembly code.
 * 
 * To compile with Emscripten:
 * emcc -o test_memory_mapping.js test_memory_mapping.c memory_mapping_utils.c \
 *     -s EMULATE_FUNCTION_POINTER_CASTS=1 \
 *     -s EXPORTED_FUNCTIONS="['_test_buffer_operations']" \
 *     -s EXPORTED_RUNTIME_METHODS="['ccall','cwrap']"
 * 
 * To run tests from JavaScript:
 * Module._test_buffer_operations();
 */

#include "memory_mapping_utils.h"

// Try to include emscripten.h, but don't fail if not available
#if defined(__EMSCRIPTEN__)
#include <emscripten.h>
#endif

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/**
 * @brief Print buffer information
 */
void print_buffer_info(jobject buffer) {
    void* addr = cheerpj_get_buffer_address(buffer);
    int capacity = cheerpj_get_buffer_capacity(buffer);
    
    printf("Buffer Info:\n");
    printf("  Address: 0x%p\n", addr);
    printf("  Capacity: %d bytes\n", capacity);
}

/**
 * @brief Test reading from a buffer
 * 
 * This demonstrates how to read data from a CheerpJ ByteBuffer
 * into a C array.
 * 
 * @param buffer The CheerpJ ByteBuffer to read from
 */
void test_buffer_read(jobject buffer) {
    printf("\n=== Test: Buffer Read ===\n");
    print_buffer_info(buffer);
    
    int capacity = cheerpj_get_buffer_capacity(buffer);
    if (capacity == 0) {
        printf("Buffer has zero capacity, skipping read test.\n");
        return;
    }
    
    // Allocate destination buffer
    int read_size = (capacity < 256) ? capacity : 256;
    uint8_t* dest = (uint8_t*)malloc(read_size);
    
    if (dest == NULL) {
        printf("ERROR: Failed to allocate destination buffer.\n");
        return;
    }
    
    // Read first 'read_size' bytes from buffer
    cheerpj_read_buffer(buffer, dest, 0, read_size);
    
    // Print the data
    printf("Read %d bytes from buffer:\n", read_size);
    printf("Data: ");
    for (int i = 0; i < read_size; i++) {
        printf("%02x ", dest[i]);
        if ((i + 1) % 16 == 0) {
            printf("\n      ");
        }
    }
    printf("\n");
    
    free(dest);
}

/**
 * @brief Test writing to a buffer
 * 
 * This demonstrates how to write data from a C array
 * into a CheerpJ ByteBuffer.
 * 
 * @param buffer The CheerpJ ByteBuffer to write to
 */
void test_buffer_write(jobject buffer) {
    printf("\n=== Test: Buffer Write ===\n");
    print_buffer_info(buffer);
    
    int capacity = cheerpj_get_buffer_capacity(buffer);
    if (capacity == 0) {
        printf("Buffer has zero capacity, skipping write test.\n");
        return;
    }
    
    // Create source data (a simple pattern)
    int write_size = (capacity < 128) ? capacity : 128;
    uint8_t* src = (uint8_t*)malloc(write_size);
    
    if (src == NULL) {
        printf("ERROR: Failed to allocate source buffer.\n");
        return;
    }
    
    // Fill with pattern: 0, 1, 2, ..., 127, 0, 1, 2, ...
    for (int i = 0; i < write_size; i++) {
        src[i] = i % 128;
    }
    
    // Write to buffer
    cheerpj_write_buffer(buffer, src, 0, write_size);
    printf("Wrote %d bytes to buffer at offset 0.\n", write_size);
    
    // Verify by reading back
    uint8_t* verify = (uint8_t*)malloc(write_size);
    if (verify != NULL) {
        cheerpj_read_buffer(buffer, verify, 0, write_size);
        
        int match = memcmp(src, verify, write_size);
        printf("Verification: %s\n", match == 0 ? "PASSED" : "FAILED");
        
        free(verify);
    }
    
    free(src);
}

/**
 * @brief Test offset-based operations
 * 
 * This demonstrates reading/writing with non-zero offsets.
 * 
 * @param buffer The CheerpJ ByteBuffer to test with
 */
void test_buffer_offset_ops(jobject buffer) {
    printf("\n=== Test: Offset Operations ===\n");
    print_buffer_info(buffer);
    
    int capacity = cheerpj_get_buffer_capacity(buffer);
    if (capacity < 64) {
        printf("Buffer too small for offset test, skipping.\n");
        return;
    }
    
    // Write pattern at offset 16
    uint8_t pattern[32];
    for (int i = 0; i < 32; i++) {
        pattern[i] = 0xFF - i;
    }
    
    cheerpj_write_buffer(buffer, pattern, 16, 32);
    printf("Wrote 32 bytes at offset 16.\n");
    
    // Read back from offset 20
    uint8_t read_back[20];
    cheerpj_read_buffer(buffer, read_back, 20, 20);
    
    printf("Read 20 bytes from offset 20:\n");
    printf("Data: ");
    for (int i = 0; i < 20; i++) {
        printf("%02x ", read_back[i]);
    }
    printf("\n");
    
    // Verify: read_back[0] should be pattern[4] = 0xFF - 4 = 0xFB
    if (read_back[0] == 0xFB) {
        printf("Verification: PASSED\n");
    } else {
        printf("Verification: FAILED (expected 0xFB, got 0x%02x)\n", read_back[0]);
    }
}

/**
 * @brief Test bounds checking
 * 
 * This demonstrates that the utilities properly handle
 * out-of-bounds access attempts.
 * 
 * @param buffer The CheerpJ ByteBuffer to test with
 */
void test_bounds_checking(jobject buffer) {
    printf("\n=== Test: Bounds Checking ===\n");
    print_buffer_info(buffer);
    
    int capacity = cheerpj_get_buffer_capacity(buffer);
    uint8_t src[16] = {0};
    uint8_t dest[16] = {0};
    
    // Test 1: Read beyond capacity (should clamp)
    printf("Test 1: Read %d bytes from offset %d...\n", 16, capacity - 4);
    cheerpj_read_buffer(buffer, dest, capacity - 4, 16);
    printf("  Should have clamped to capacity without crashing.\n");
    
    // Test 2: Write beyond capacity (should clamp)
    printf("Test 2: Write %d bytes to offset %d...\n", 16, capacity - 4);
    cheerpj_write_buffer(buffer, src, capacity - 4, 16);
    printf("  Should have clamped to capacity without crashing.\n");
    
    // Test 3: Negative offset (should be handled gracefully)
    printf("Test 3: Read with negative offset...\n");
    cheerpj_read_buffer(buffer, dest, -1, 8);
    printf("  Should have handled gracefully.\n");
    
    printf("Bounds checking tests completed without crashes.\n");
}

/**
 * @brief Main test function
 * 
 * This function is exported and can be called from JavaScript
 * to run all tests on a CheerpJ ByteBuffer.
 * 
 * @param buffer A CheerpJ ByteBuffer object to test with
 */
EMSCRIPTEN_KEEPALIVE void test_buffer_operations(jobject buffer) {
    printf("\n========================================\n");
    printf("  CheerpJ Buffer Mapping Utilities Test\n");
    printf("========================================\n");
    
    if (buffer == NULL) {
        printf("ERROR: Buffer is NULL.\n");
        printf("\nUsage: Pass a CheerpJ ByteBuffer object to this function.\n");
        printf("Example (JavaScript):\n");
        printf("  var buffer = ByteBuffer.allocateDirect(256);\n");
        printf("  Module._test_buffer_operations(buffer);\n");
        return;
    }
    
    // Run all tests
    test_buffer_read(buffer);
    test_buffer_write(buffer);
    test_buffer_offset_ops(buffer);
    test_bounds_checking(buffer);
    
    printf("\n========================================\n");
    printf("  All tests completed!\n");
    printf("========================================\n");
}

/**
 * @brief Example: Using buffer data with OpenGL
 * 
 * This demonstrates how to use buffer mapping utilities
 * to transfer data to OpenGL functions.
 * 
 * @param texture_buffer ByteBuffer containing texture data
 * @param vbo_buffer ByteBuffer containing vertex data
 */
EMSCRIPTEN_KEEPALIVE void example_opengl_usage(jobject texture_buffer, jobject vbo_buffer) {
    printf("\n=== Example: OpenGL Buffer Usage ===\n");
    
    // Get texture data
    void* tex_data = cheerpj_get_buffer_address(texture_buffer);
    int tex_size = cheerpj_get_buffer_capacity(texture_buffer);
    printf("Texture data: address=0x%p, size=%d\n", tex_data, tex_size);
    
    // Would call OpenGL here (pseudo-code):
    // glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, 
    //              GL_RGBA, GL_UNSIGNED_BYTE, tex_data);
    
    // Get vertex data
    void* vbo_data = cheerpj_get_buffer_address(vbo_buffer);
    int vbo_size = cheerpj_get_buffer_capacity(vbo_buffer);
    printf("VBO data: address=0x%p, size=%d\n", vbo_data, vbo_size);
    
    // Would call OpenGL here (pseudo-code):
    // glBufferData(GL_ARRAY_BUFFER, vbo_size, vbo_data, GL_STATIC_DRAW);
    
    printf("In actual usage, these addresses would be passed to OpenGL functions.\n");
}

/**
 * @brief Simple API test without actual buffer data
 * 
 * This function tests that all functions compile and can be called
 * without crashing, even with NULL or invalid buffers.
 */
EMSCRIPTEN_KEEPALIVE void test_api_safety() {
    printf("\n=== Test: API Safety ===\n");
    
    // Test with NULL buffer - should handle gracefully
    void* addr = cheerpj_get_buffer_address(NULL);
    printf("Get address from NULL: %p\n", addr);
    
    int cap = cheerpj_get_buffer_capacity(NULL);
    printf("Get capacity from NULL: %d\n", cap);
    
    uint8_t data[10] = {0};
    cheerpj_read_buffer(NULL, data, 0, 10);
    printf("Read from NULL: handled gracefully\n");
    
    cheerpj_write_buffer(NULL, data, 0, 10);
    printf("Write to NULL: handled gracefully\n");
    
    printf("API safety test passed!\n");
}

/**
 * @brief Entry point for native testing
 * 
 * This can be called to run basic API tests
 * when not running in a browser/CheerpJ environment.
 */
int main() {
    printf("\nRunning API safety tests...\n");
    test_api_safety();
    
    printf("\nNote: Full buffer tests require actual CheerpJ buffers.\n");
    printf("The test_buffer_operations() function should be called from\n");
    printf("JavaScript with real CheerpJ ByteBuffer objects.\n");
    
    return 0;
}
