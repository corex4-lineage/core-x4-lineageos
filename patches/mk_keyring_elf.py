import struct, sys
def u32(x): return struct.pack('<I', x)
BASE = 0x400000; CODE_OFF = 64 + 56
def adr(rd, imm): return 0x10000000 | ((imm & 3) << 29) | ((imm >> 2) << 5) | rd
def movz(rd, imm): return 0xD2800000 | (imm << 5) | rd
def movn(rd, imm): return 0x92800000 | (imm << 5) | rd
SVC = 0xD4000001
code = [adr(0, 36), adr(1, 40), movz(2, 0), movz(3, 0), movn(4, 2),   # x4 = -3 = KEY_SPEC_SESSION_KEYRING
        movz(8, 217), SVC,                                             # add_key
        movz(8, 93), SVC]                                              # exit(x0)
body = b''.join(u32(i) for i in code)
assert len(body) == 36
body += b'keyring\0' + b'fscrypt\0'
total = CODE_OFF + len(body)
ehdr = b'\x7fELF' + bytes([2, 1, 1, 0]) + b'\0' * 8
ehdr += struct.pack('<HHIQQQIHHHHHH', 2, 0xB7, 1, BASE + CODE_OFF, 64, 0, 0, 64, 56, 1, 0, 0, 0)
phdr = struct.pack('<IIQQQQQQ', 1, 5, 0, BASE, BASE, total, total, 0x1000)
data = ehdr + phdr + body
assert len(ehdr) == 64 and len(phdr) == 56
open(sys.argv[1], 'wb').write(data)
print('ecrit', sys.argv[1], len(data), 'octets')
