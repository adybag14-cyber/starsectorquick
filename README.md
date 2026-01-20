# Memory Mapping Utilities for CheerpJ Buffers

Utilities for mapping Java ByteBuffer objects to C pointers in CheerpJ/WebAssembly environments. These utilities enable efficient data transfer between Java and C, particularly for buffer-based OpenGL functions like `glTexImage2D`, `glBufferData`, and `glBufferSubData`.

## Overview

This module provides C functions to interact with CheerpJ ByteBuffer objects from WebAssembly code, allowing you to:

- Get the memory address of a buffer's underlying data
- Get buffer capacity/size
- Read data from a buffer into a C array
- Write data from a C array into a buffer

These utilities are essential for high-performance graphics operations where data needs to be transferred efficiently between Java and OpenGL.

## Use Cases

- **Texture Data**: Transfer image data from Java ByteBuffers to OpenGL via `glTexImage2D`
- **Vertex Buffer Objects**: Upload vertex data to GPU via `glBufferData`
- **Index Buffer Objects**: Upload index data to GPU
- **Shader Uniforms**: Transfer uniform block data
- **Any buffer-based OpenGL operation**: Any function requiring a data pointer

## Files

- `memory_mapping_utils.h` - Header file with function declarations
- `memory_mapping_utils.c` - Implementation of buffer mapping functions
- `test_memory_mapping.c` - Test and demonstration code

## API Reference

### Functions

#### `void* cheerpj_get_buffer_address(jobject buffer)`

Get the memory address of a CheerpJ ByteBuffer's underlying data.

**Parameters:**
- `buffer` - The CheerpJ ByteBuffer object

**Returns:**
- Pointer to the buffer's data in WebAssembly memory, or `NULL` on error

**Notes:**
- For direct buffers, returns the actual data address
- For non-direct buffers, returns the address of the backing array's data
- The returned pointer is only valid while the buffer is not garbage collected

#### `int cheerpj_get_buffer_capacity(jobject buffer)`

Get the capacity/size of a CheerpJ ByteBuffer in bytes.

**Parameters:**
- `buffer` - The CheerpJ ByteBuffer object

**Returns:**
- Buffer capacity in bytes, or `0` on error

#### `void cheerpj_read_buffer(jobject buffer, void* dest, int offset, int length)`

Read data from a CheerpJ ByteBuffer into a C array.

**Parameters:**
- `buffer` - The CheerpJ ByteBuffer object
- `dest` - Pointer to destination C array
- `offset` - Starting offset in buffer (in bytes)
- `length` - Number of bytes to read

**Notes:**
- Performs bounds checking - will not read beyond buffer capacity
- If `offset + length` exceeds capacity, reads up to capacity only

#### `void cheerpj_write_buffer(jobject buffer, void* src, int offset, int length)`

Write data from a C array into a CheerpJ ByteBuffer.

**Parameters:**
- `buffer` - The CheerpJ ByteBuffer object
- `src` - Pointer to source C array
- `offset` - Starting offset in buffer (in bytes)
- `length` - Number of bytes to write

**Notes:**
- Performs bounds checking - will not write beyond buffer capacity
- If `offset + length` exceeds capacity, writes up to capacity only

## Usage Examples

### Example 1: Getting Buffer Information

```c
#include "memory_mapping_utils.h"

void inspect_buffer(jobject buffer) {
    void* addr = cheerpj_get_buffer_address(buffer);
    int capacity = cheerpj_get_buffer_capacity(buffer);
    
    printf("Buffer address: %p\n", addr);
    printf("Buffer capacity: %d bytes\n", capacity);
}
```

### Example 2: Using with OpenGL (glBufferData)

```c
#include "memory_mapping_utils.h"

// Assume these are defined elsewhere
extern void glBufferData(int target, int size, void* data, int usage);

void upload_vertex_data(jobject buffer) {
    void* data = cheerpj_get_buffer_address(buffer);
    int size = cheerpj_get_buffer_capacity(buffer);
    
    if (data != NULL && size > 0) {
        glBufferData(GL_ARRAY_BUFFER, size, data, GL_STATIC_DRAW);
    }
}
```

### Example 3: Using with OpenGL (glTexImage2D)

```c
#include "memory_mapping_utils.h"

// Assume these are defined elsewhere
extern void glTexImage2D(int target, int level, int internalformat,
                        int width, int height, int border,
                        int format, int type, void* pixels);

void upload_texture(jobject texture_buffer, int width, int height) {
    void* pixels = cheerpj_get_buffer_address(texture_buffer);
    
    if (pixels != NULL) {
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,
                     GL_RGBA, GL_UNSIGNED_BYTE, pixels);
    }
}
```

### Example 4: Reading/Modifying Buffer Data

```c
#include "memory_mapping_utils.h"
#include <string.h>

void invert_colors(jobject buffer) {
    int capacity = cheerpj_get_buffer_capacity(buffer);
    
    // Read entire buffer
    uint8_t* temp = (uint8_t*)malloc(capacity);
    cheerpj_read_buffer(buffer, temp, 0, capacity);
    
    // Modify data (invert colors: RGBA -> (255-R, 255-G, 255-B, A))
    for (int i = 0; i < capacity; i += 4) {
        temp[i + 0] = 255 - temp[i + 0]; // R
        temp[i + 1] = 255 - temp[i + 1]; // G
        temp[i + 2] = 255 - temp[i + 2]; // B
        // Alpha (i+3) stays the same
    }
    
    // Write back
    cheerpj_write_buffer(buffer, temp, 0, capacity);
    
    free(temp);
}
```

### Example 5: Partial Buffer Operations

```c
void update_vertex_subset(jobject buffer, int offset, void* new_vertices, int size) {
    // Update only a portion of the buffer
    cheerpj_write_buffer(buffer, new_vertices, offset, size);
}

void read_first_row(jobject texture_buffer, int row_size, uint8_t* output) {
    // Read only the first row of a texture
    cheerpj_read_buffer(texture_buffer, output, 0, row_size);
}
```

## Compilation

### Emscripten Compilation

To compile the utilities with Emscripten:

```bash
# Compile the utilities
emcc -c memory_mapping_utils.c -o memory_mapping_utils.o

# Compile with your project
emcc -o output.js your_code.c memory_mapping_utils.o \
    -s EMULATE_FUNCTION_POINTER_CASTS=1 \
    -s EXPORTED_FUNCTIONS="[
        '_cheerpj_get_buffer_address',
        '_cheerpj_get_buffer_capacity',
        '_cheerpj_read_buffer',
        '_cheerpj_write_buffer'
    ]"
```

### Running Tests

To compile and run the test file:

```bash
# Compile test
emcc -o test_memory_mapping.js \
    test_memory_mapping.c \
    memory_mapping_utils.c \
    -s EMULATE_FUNCTION_POINTER_CASTS=1 \
    -s EXPORTED_FUNCTIONS="[
        '_test_buffer_operations',
        '_test_with_simulated_buffer'
    ]" \
    -s EXPORTED_RUNTIME_METHODS="['ccall','cwrap']"

# Run with simulated buffer (no CheerpJ required)
node test_memory_mapping.js
```

### Integration with CheerpJ

When using with CheerpJ:

1. Compile the utilities to WebAssembly with Emscripten
2. Load the compiled `.js` file in your CheerpJ application
3. Call the exported functions from Java using JNI
4. Pass ByteBuffer objects from Java to the native functions

## CheerpJ Buffer Assumptions

These utilities make certain assumptions about how CheerpJ represents ByteBuffer objects in memory:

### Direct ByteBuffers

```
Offset | Type     | Description
-------|----------|------------------------------------
+0     | pointer  | vtable pointer
+4     | int32    | isDirect flag (1 for direct buffers)
+8     | int32    | capacity (in bytes)
+12    | int32    | current position
+16    | int32    | current limit
+20    | pointer  | backingArray (NULL for direct)
+24    | int32    | arrayOffset
+28    | pointer  | directBufferAddress (data address)
```

### Non-Direct ByteBuffers

```
Offset | Type     | Description
-------|----------|------------------------------------
+0     | pointer  | vtable pointer
+4     | int32    | isDirect flag (0 for non-direct)
+8     | int32    | capacity (in bytes)
+12    | int32    | current position
+16    | int32    | current limit
+20    | pointer  | backingArray (Java byte[] object)
+24    | int32    | arrayOffset
```

### Java Byte Arrays

For non-direct buffers, the backing byte array has this structure:

```
Offset | Type     | Description
-------|----------|------------------------------------
+0     | int32    | array length
+4     | bytes[]  | actual array data
```

### Important Notes

1. **Offset Values**: The offsets used in the implementation may need adjustment based on your specific CheerpJ version and configuration. If buffer access fails, check the actual object layout in your CheerpJ build.

2. **Direct Buffers Recommended**: For best performance with OpenGL, use direct ByteBuffers (`ByteBuffer.allocateDirect()`). Non-direct buffers require extra indirection.

3. **Garbage Collection**: The returned address from `cheerpj_get_buffer_address()` is only valid while the buffer is not garbage collected. Keep a reference to the buffer from Java while using the address.

4. **Thread Safety**: These functions are not thread-safe. Use proper synchronization if accessing buffers from multiple threads.

## Error Handling

All functions perform basic error checking:

- `NULL` buffer objects return `NULL` or `0`
- Invalid offsets are handled gracefully
- Out-of-bounds operations are clamped to buffer capacity
- NULL destination/source pointers are handled without crashes

## Performance Considerations

- Direct buffers are faster (single memory access)
- Non-direct buffers require extra indirection
- Use `cheerpj_get_buffer_address()` + direct memory access for bulk operations
- Use `cheerpj_read_buffer()`/`cheerpj_write_buffer()` for smaller operations

## Related Issues

- **bd-6**: Texture functions - uses these utilities for glTexImage2D
- **bd-7**: VBO functions - uses these utilities for glBufferData

## License

Part of the StarSector 2 project.

## Contributing

When modifying these utilities:

1. Maintain backward compatibility with existing CheerpJ buffer layouts
2. Add tests for new functionality in `test_memory_mapping.c`
3. Update this README with new usage examples
4. Document any changes to buffer offset assumptions

## Troubleshooting

### Problem: Buffer address is NULL

**Possible causes:**
- CheerpJ version has different buffer layout
- Buffer was garbage collected
- Buffer is not properly initialized

**Solution:** Check CheerpJ object layout, verify buffer is alive, ensure proper initialization.

### Problem: Reading/Writing incorrect data

**Possible causes:**
- Buffer offsets are incorrect for your CheerpJ version
- Mix of direct/non-direct buffers
- Endianness issues

**Solution:** Verify buffer structure matches assumptions, check isDirect flag, verify data endianness.

### Problem: Crashes on buffer access

**Possible causes:**
- Buffer was freed/garbage collected
- Memory corruption
- Incorrect buffer object passed

**Solution:** Ensure buffer remains referenced from Java, verify valid buffer object, check for memory issues.
