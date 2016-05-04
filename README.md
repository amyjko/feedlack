FeedLack is a program analysis for finding missing user feedback in web applications. It is a research prototype described in this paper

[FeedLack Detects Missing Feedback in Web Applications](http://faculty.washington.edu/ajko/publications?id=feedlack). Ko, A.J. and Zhang, X. (2011). ACM Conference on Human Factors in Computing Systems (CHI), 2177-2186.

# History

Back in 2010 I went to a [Dagstuhl](https://www.dagstuhl.de/) workshop titled "Practical Software Testing". People from all over the world described some amazing work verifying a wide range of properties, including correctness, security, robustness, and performance. As one of the few HCI people at the conference, however, it struck me how there seemed to be little work to verify, formally, the _usability_ properties of software. Were there any properties amenable to formal verification, and if so, how would we verify them?

During the workshop, the one that came to mind was **feedback**. A widely considered usability principle is that for every user input, there should be a corresponding visible program output that communicates to the user that the input was received and that the program did something with it (or ignored it). Was it possible to prove (or disprove) that a program always does this?

It turns out, yes. The basic idea is that _all possible paths_ through a user interface starting from user input should have at least one output affecting statement. FeedLack is the algorithm that verifies that this is true. If it's not true, it finds counterexamples, which can be thought of as usability software defects that should be fixed by adding output.

# Architecture

The architecture is pretty simple:

* Take as input a web application
* FeedLack analyzes all of the JavaScript source in the application, building a call graph (kind of like Facebook's [Flow](https://code.facebook.com/projects/1524880081090726/) does).
* Traverse all non-infinite paths through the call graph that start from user input
* Generate a warning for each path that lacks an output affecting statement

# Support

Unfortunately, because I've long since moved on to other projects, I cannot support this code or develop it further. Fork it, patch it, extend it: do whatever you like with it. It's here for the public good as an archive for future generations of developer tool developers. I'd love to see what you do with it! I love to hear stories about how people are building upon the work.

That said, if you find that things are critically broken and can be fixed with some simple changes, submit a pull request. I'll review all requests eventually and merge them, so that others can continue to play with the code.
