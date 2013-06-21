## Description

TODO

## Install

Install aspell:

```sh
# For mac osx
$ brew install aspell
```

Until this is released as a clojar, clone the repo and `lein install`.

Add to your project.clj:

    [lein-spell "0.1.0-SNAPSHOT"]

## Usage

To find misspelled words:
```sh
$ lein spell
associtaed
...
```

By default var/fn docstrings under src/ and files that match **/*.{md,txt}
are searched for typos.

If you want to spellcheck certain files, just pass them as arguments:

```sh
$ lein spell README.md src/leiningen/spell.clj
...
```

Realistically, this spellchecker will find false positives as there
are many words we use that aren't recognized by traditional dictionaries.
Therefore, it's encouraged to create your own local whitelist as you find
false positives:

```sh
# If you don't have tee: lein spell 1> .lein-spell
$ lein spell | tee .lein-spell
associtaed
bugfix
communitcated
deployable
specfy
tranisition
```

lein-spell recognizes `.lein-spell` as a local whitelist of words that are spelled correctly. In
the above example, the words `bugfix` and `deployable` are correct so we only keep those two in the
whitelist.

If we run `lein spell` again, only the misspelled words print out. Once we fix those typos,
subsequent runs of `lein spell` return empty until new typos are introduced.

If you'd like to contribute your whitelist to lein-spell, see [Conributing](#contributing)

## Bugs/Issues

Please report them [on github](http://github.com/cldwalker/lein-spell/issues).

## Contributing
To contribute your whitelist, please just add them to the end of the [main whitelist](#TODO). This
makes it easier for me to curate the list.

If not contributing to the whitelist, [see here for contribution
guidelines.](http://tagaholic.me/contributing.html)

## License

See LICENSE.TXT

## TODO
* Consider a java spellchecking library if it's as good as aspell
* Improve whitelist
** Add core java classes and methods
** Add a project's dependency names
* Search namespace docstrings for typos
