#!/usr/bin/env python
from optparse import OptionParser
from os import walk, path, makedirs
import hashlib


def md5_for_file(path, block_size=256 * 128):
    '''
    Block size directly depends on the block size of your filesystem
    to avoid performances issues
    Here I have blocks of 4096 octets (Default NTFS)
    '''
    md5 = hashlib.md5()
    with open(path, 'rb') as f:
        for chunk in iter(lambda: f.read(block_size), b''):
            md5.update(chunk)
    return md5.hexdigest()


class gen_xml4jupar:
    exclude_dirs = ['maven-archiver', 'generated-sources', 'classes', 'update_info', 'site', 'antrun']


    def gen_update(self):
        '''
        <?xml version="1.0" encoding="UTF-8"?>
        <update>
            <instruction>
            <action>MOVE</action>
            <file>JupDemo.jar</file>
            <destination>JupDemo.jar</destination>
            </instruction>
        </update>
        '''
        name = 'update.xml'
        content = '''\
<?xml version="1.0" encoding="UTF-8"?>
<update>'''

        for absfile, relfile in self.files:
            if path.basename(relfile) != 'update.xml':
                content += '''
    <instruction>
        <action>MOVE</action>
        <file>%s</file>
        <destination>%s</destination>
    </instruction>''' % (path.basename(relfile), relfile)

        content += '''
</update>'''

        return name, content

    def gen_latest(self):
        '''
        <?xml version="1.0" encoding="UTF-8"?>
        <information>
            <pubDate>Sat, 24 Dec 2011 19:58:42 +0200</pubDate>
            <pkgver>1.0</pkgver>
            <pkgrel>2</pkgrel>
            <severity>normal</severity>
            <extra>
                <message></message>
            </extra>
        </information>
        '''
        name = 'latest.xml'
        content = '''\
<?xml version="1.0" encoding="UTF-8"?>
<information>
    <pubDate>%s</pubDate>
    <pkgver>%s</pkgver>
    <pkgrel>%s</pkgrel>
    <severity>normal</severity>
    <extra>
        <message></message>
    </extra>
</information>''' % (self.opts['pubDate'], self.opts['pkgver'], self.opts['pkgrel'])

        return name, content


    def gen_files(self):
        '''
        <?xml version="1.0" encoding="UTF-8"?>
        <update>
            <file md5="d86bc330309d2956ff2fe1947348a0d5">http://niovi.aueb.gr/~p3070130/JupDemo.jar</file>
            <file md5="4286711776c4ed40cca7e8fc6eb90b1c">http://niovi.aueb.gr/~p3070130/update.xml</file>
        </update>
        '''

        name = 'files.xml'
        content = '''\
<?xml version="1.0" encoding="UTF-8"?>
<update>'''

        for absfile, relfile in self.files:
            content += '''
    <file%s>%s</file>''' % ((' md5="%s"' % md5_for_file(absfile) if options.crc else "") or "", path.join(self.server_prefix, relfile))

        content += '''
</update>'''

        return name, content

    def __init__(self, *args, **kwargs):
        self.configure(*args, **kwargs)

    def configure(self, options, args):
        print 'options: %s' % options
        print 'args: %s' % args
        self.source = path.abspath(options.source)
        self.dest = path.abspath(options.dest)
        self.crc = True if options.crc else False

        self.opts = {
            'pubDate': options.pubDate or 'Sat, 24 Dec 2011 19:58:42 +0200',
            'pkgver': options.pkgver or '1.0',
            'pkgrel': options.pkgrel or '2',
        }

        self.server_prefix = options.server_prefix or ('file:///%s' % path.abspath(self.source))

    def build(self):
        self.files = []
        for (dirpath, dirnames, filenames) in walk(self.source):
            relpath = path.relpath(dirpath, self.source)
            if relpath.startswith('.'):
                if relpath == '.':
                    relpath = ''
                else:
                    continue
            if relpath.split(path.sep)[0] in self.exclude_dirs:
                continue

            self.files.extend(map(lambda x: (path.abspath(path.join(dirpath, x)), path.join(relpath, x)), filenames))

        self.files.append((path.abspath(path.join(self.dest, 'update.xml')), 'update_info/update.xml'))

        if not path.exists(self.dest):
            makedirs(self.dest)

        for func in (self.gen_update, self.gen_latest, self.gen_files):
            name, content = func()
            with open(path.join(self.dest, name), 'wt') as f:
                f.write(content)
                f.flush()
                f.close()


if __name__ == '__main__':
    parser = OptionParser()
    parser.add_option("-s", "--source", dest="source",
                      help="use files from source directory", metavar="SOURCE")
    parser.add_option("-d", "--dest", dest="dest",
                      help="write report to destination direcrory", metavar="DEST")
    parser.add_option("-p", "--prefix", dest="server_prefix",
                      help="prefix", metavar="prefix")
    parser.add_option("-Q", "--pubDate", dest="pubDate",
                      help="pubDate", metavar="pubDate")
    parser.add_option("-W", "--pkgver", dest="pkgver",
                      help="pkgver", metavar="pkgver")
    parser.add_option("-E", "--pkgrel", dest="pkgrel",
                      help="pkgver",    metavar="pkgrel")
    parser.add_option("-c", "--crc", dest="crc",
                      help="crc", metavar="crc")
    (options, args) = parser.parse_args()
    gen_xml4jupar(options, args).build()