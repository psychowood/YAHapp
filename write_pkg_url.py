
from __future__ import with_statement
from __future__ import absolute_import
from sys import argv, exit
from io import open

def main():
  if len(argv) != 3:
    print u"Usage: write_url.py filename url"
    return -2
  if len(argv[2]) >= 255:
    print u"url must be at most 255 characters!"
    return -2
  with open(argv[1], u"r+b") as file:
    data = file.read()
    pos = data.find(str('x'*256))
    file.seek(pos, 0)
    file.write(argv[2].encode(u'ascii'))
    file.write(str('\0' * (256 - len(argv[2]))))

if __name__ == u"__main__":
  main()
