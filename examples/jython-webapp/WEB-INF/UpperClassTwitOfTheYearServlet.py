from freemarker.ext import servlet

class UpperClassTwitOfTheYearServlet(servlet.FreemarkerServlet):
    def initializeServletContext(self, request, response):
        twits = {'Vivian Smith-Smythe-Smith of Kensington' : ('O-level in chemo-hygiene', 'shooting himself', 'runner up'), \
                 'Simon Zinc-Trumpet-Harris': ('married to a very attractive table lamp', 'being shot by Nigel', 'not known'), \
                 'Nigel Incubator-Jones of Henley': ('his best friend is a tree and in his spare time he \'s a stockbroker', 'shooting himself', 'third place'), \
                 'Gervaise Brook-Hampster of Kensington and Weybridge': ('is in the Guards and his father uses him as a wastebasket', 'shooting himself', 'winner'), \
                 'Oliver St John Mollusc': ('Harrow and the Guards, thought by many to be this year\'s outstanding twit', 'running himself over with a car', 'not known') \
                }
        self.servletContext.setAttribute('twits', twits)


