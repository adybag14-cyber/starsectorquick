#ifndef MEMORY_MAPPING_UTILS_H
#define MEMORY_MAPPING_UTILS_H

/**
 * @file memory_mapping_utils.h
 * @brief Utilities for mapping Java ByteBuffers to C pointers in CheerpJ/WebAssembly
 * 
 * This header provides functions to interact with CheerpJ ByteBuffer objects
 * from C/WebAssembly code, enabling efficient data transfer between Java and C.
 * 
 * These utilities are essential for buffer-based OpenGL functions like:
 * - glTexImage2D (texture data)
 * - glBufferData (VBO/IBO data)
 * - glBufferSubData (buffer updates)
 * 
 * @note This module assumes CheerpJ ByteBuffer objects have the following structure:
 *       - The object is a pointer to CheerpJ object metadata
 *       - Direct buffers store their data as a contiguous block in WebAssembly memory
 *       - Non-direct buffers use Java arrays that may need conversion
 */

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>

// Define EMSCRIPTEN_KEEPALIVE for non-Emscripten environments
#ifndef EMSCRIPTEN_KEEPALIVE
#define EMSCRIPTEN_KEEPALIVE
#endif

// JNI types (simplified for CheerpJ/WebAssembly context)
typedef void* jobject;

/**
 * @brief Get the memory address of a CheerpJ ByteBuffer's data
 * 
 * Extracts the underlying memory address of the buffer's backing array.
 * For direct buffers, this returns the actual data address.
 * For non-direct buffers, behavior may vary.
 * 
 * @param buffer The CheerpJ ByteBuffer object
 * @return void* Pointer to the buffer's data in WebAssembly memory, or NULL on error
 * 
 * @note The returned pointer is only valid as long as the buffer is not garbage collected
 */
EMSCRIPTEN_KEEPALIVE void* cheerpj_get_buffer_address(jobject buffer);

/**
 * @brief Get the capacity/size of a CheerpJ ByteBuffer
 * 
 * Returns the total capacity of the buffer in bytes.
 * 
 * @param buffer The CheerpJ ByteBuffer object
 * @return int Buffer capacity in bytes, or 0 on error
 */
EMSCRIPTEN_KEEPALIVE int cheerpj_get_buffer_capacity(jobject buffer);

/**
 * @brief Read data from a CheerpJ ByteBuffer into a C array
 * 
 * Copies 'length' bytes from the buffer starting at 'offset' into the
 * destination C array.
 * 
 * @param buffer The CheerpJ ByteBuffer object
 * @param dest Pointer to destination C array
 * @param offset Starting offset in buffer (in bytes)
 * @param length Number of bytes to read
 * 
 * @note Ensures bounds checking - will not read beyond buffer capacity
 */
EMSCRIPTEN_KEEPALIVE void cheerpj_read_buffer(jobject buffer, void* dest, int offset, int length);

/**
 * @brief Write data from a C array into a CheerpJ ByteBuffer
 * 
 * Copies 'length' bytes from the source C array into the buffer
 * starting at 'offset'.
 * 
 * @param buffer The CheerpJ ByteBuffer object
 * @param src Pointer to source C array
 * @param offset Starting offset in buffer (in bytes)
 * @param length Number of bytes to write
 * 
 * @note Ensures bounds checking - will not write beyond buffer capacity
 */
EMSCRIPTEN_KEEPALIVE void cheerpj_write_buffer(jobject buffer, void* src, int offset, int length);

#ifdef __cplusplus
}
#endif

#endif // MEMORY_MAPPING_UTILS_H
