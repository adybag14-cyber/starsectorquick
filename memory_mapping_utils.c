/**
 * @file memory_mapping_utils.c
 * @brief Implementation of CheerpJ buffer mapping utilities
 * 
 * This module provides functions to access CheerpJ ByteBuffer objects
 * from C/WebAssembly code for efficient data transfer.
 */

#include "memory_mapping_utils.h"

// Try to include emscripten.h, but don't fail if not available
#if defined(__EMSCRIPTEN__)
#include <emscripten.h>
#endif

#include <stdint.h>
#include <string.h>

/**
 * CheerpJ ByteBuffer internal structure (conceptual)
 * 
 * Note: The exact layout depends on CheerpJ version and configuration.
 * This implementation uses a generic approach that should work with
 * most CheerpJ buffer representations.
 * 
 * Typical CheerpJ ByteBuffer layout:
 * - Offset +0: Object header/vtable pointer
 * - Offset +4: isDirect flag (int)
 * - Offset +8: capacity (int)
 * - Offset +12: position (int)
 * - Offset +16: limit (int)
 * - Offset +20: backingArray (pointer to byte array)
 * - Offset +24: arrayOffset (int)
 * 
 * For direct buffers:
 * - Offset +28: directBufferAddress (void*)
 * 
 * Note: These offsets are approximate and may need adjustment based on
 * actual CheerpJ implementation.
 */

// Offset constants for ByteBuffer fields (may need adjustment)
#define OFFSET_CAPACITY      8
#define OFFSET_IS_DIRECT     4
#define OFFSET_BACKING_ARRAY 20
#define OFFSET_ARRAY_OFFSET  24
#define OFFSET_DIRECT_ADDR   28

/**
 * @brief Helper to read an int field from a CheerpJ object
 */
static int cheerpj_get_int_field(jobject obj, int offset) {
    if (obj == NULL) {
        return 0;
    }
    int32_t* ptr = (int32_t*)((uintptr_t)obj + offset);
    return *ptr;
}

/**
 * @brief Helper to read a pointer field from a CheerpJ object
 */
static void* cheerpj_get_ptr_field(jobject obj, int offset) {
    if (obj == NULL) {
        return NULL;
    }
    void** ptr = (void**)((uintptr_t)obj + offset);
    return *ptr;
}

/**
 * @brief Get the memory address of a CheerpJ ByteBuffer's data
 * 
 * For direct buffers, returns the actual WebAssembly memory address.
 * For non-direct buffers, returns the address of the backing array's data.
 * 
 * @param buffer The CheerpJ ByteBuffer object
 * @return void* Pointer to the buffer's data, or NULL on error
 */
EMSCRIPTEN_KEEPALIVE void* cheerpj_get_buffer_address(jobject buffer) {
    if (buffer == NULL) {
        return NULL;
    }
    
    // Check if this is a direct buffer
    int is_direct = cheerpj_get_int_field(buffer, OFFSET_IS_DIRECT);
    
    if (is_direct) {
        // Direct buffer - return the direct buffer address
        void* addr = cheerpj_get_ptr_field(buffer, OFFSET_DIRECT_ADDR);
        if (addr != NULL) {
            return addr;
        }
    } else {
        // Non-direct buffer - get backing array
        jobject backing_array = cheerpj_get_ptr_field(buffer, OFFSET_BACKING_ARRAY);
        if (backing_array != NULL) {
            // Get array offset
            int array_offset = cheerpj_get_int_field(buffer, OFFSET_ARRAY_OFFSET);
            
            // Get the array's data pointer
            // For CheerpJ byte arrays, the data starts at offset 4 (after array length)
            void* array_data = (void*)((uintptr_t)backing_array + 4);
            
            // Return address adjusted for array offset
            return (void*)((uintptr_t)array_data + array_offset);
        }
    }
    
    return NULL;
}

/**
 * @brief Get the capacity/size of a CheerpJ ByteBuffer
 * 
 * @param buffer The CheerpJ ByteBuffer object
 * @return int Buffer capacity in bytes, or 0 on error
 */
EMSCRIPTEN_KEEPALIVE int cheerpj_get_buffer_capacity(jobject buffer) {
    if (buffer == NULL) {
        return 0;
    }
    
    return cheerpj_get_int_field(buffer, OFFSET_CAPACITY);
}

/**
 * @brief Read data from a CheerpJ ByteBuffer into a C array
 * 
 * @param buffer The CheerpJ ByteBuffer object
 * @param dest Pointer to destination C array
 * @param offset Starting offset in buffer (in bytes)
 * @param length Number of bytes to read
 */
EMSCRIPTEN_KEEPALIVE void cheerpj_read_buffer(jobject buffer, void* dest, int offset, int length) {
    // Validate parameters
    if (buffer == NULL || dest == NULL) {
        return;
    }
    
    if (length <= 0) {
        return;
    }
    
    // Get buffer address and capacity
    void* src = cheerpj_get_buffer_address(buffer);
    int capacity = cheerpj_get_buffer_capacity(buffer);
    
    if (src == NULL) {
        return;
    }
    
    // Bounds checking
    if (offset < 0 || offset >= capacity) {
        return;
    }
    
    // Adjust length if it would exceed buffer capacity
    if (offset + length > capacity) {
        length = capacity - offset;
    }
    
    // Copy data
    memcpy(dest, (void*)((uintptr_t)src + offset), length);
}

/**
 * @brief Write data from a C array into a CheerpJ ByteBuffer
 * 
 * @param buffer The CheerpJ ByteBuffer object
 * @param src Pointer to source C array
 * @param offset Starting offset in buffer (in bytes)
 * @param length Number of bytes to write
 */
EMSCRIPTEN_KEEPALIVE void cheerpj_write_buffer(jobject buffer, void* src, int offset, int length) {
    // Validate parameters
    if (buffer == NULL || src == NULL) {
        return;
    }
    
    if (length <= 0) {
        return;
    }
    
    // Get buffer address and capacity
    void* dest = cheerpj_get_buffer_address(buffer);
    int capacity = cheerpj_get_buffer_capacity(buffer);
    
    if (dest == NULL) {
        return;
       }
    
    // Bounds checking
    if (offset < 0 || offset >= capacity) {
        return;
    }
    
    // Adjust length if it would exceed buffer capacity
    if (offset + length > capacity) {
        length = capacity - offset;
    }
    
    // Copy data
    memcpy((void*)((uintptr_t)dest + offset), src, length);
}
