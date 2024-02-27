def call() {
pipeline {
    agent any

    stages {
        stage('SCM checkout') {
    steps {
        script {
            checkout([$class: 'GitSCM',
                      branches: [[name: params.BRANCH]],
                      extensions: [],
                      userRemoteConfigs: [[url: 'https://github.com/Suryaprakash1997/my-public-repo.git']]]
            )
        }
    }
}
    stage('Dockerfile') {
    steps {
        script {
            def dockerfileExists = fileExists('Dockerfile')

            if (!dockerfileExists) {
                def dockerfileContent = """
FROM node:20-alpine AS base
            
# Production image, copy all the files and run next
FROM base AS runner
WORKDIR /app
            
ENV NODE_ENV production
# Uncomment the following line in case you want to disable telemetry during runtime.
# ENV NEXT_TELEMETRY_DISABLED 1
            
RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs
                    
# Set the correct permission for prerender cache
# RUN mkdir .next
# RUN chown nextjs:nodejs .next
                    
# Automatically leverage output traces to reduce image size
# https://nextjs.org/docs/advanced-features/output-file-tracing
COPY apps/web/.next/standalone .
RUN echo \"#!/bin/bash\n\nnode server.js\" > run.sh
                    
USER nextjs
                    
EXPOSE 3000
                   
ENV PORT 3000
# set hostname to localhost
ENV HOSTNAME "0.0.0.0"
                    
# server.js is created by next build from the standalone output
# https://nextjs.org/docs/pages/api-reference/next-config-js/output
CMD ["node", "apps/web/server.js"]
"""
                
                // Create Dockerfile only if it doesn't exist
                writeFile file: 'Dockerfile', text: dockerfileContent
            } else {
                echo 'Dockerfile already exists. Skipping creation.'
            }

            sh 'cat Dockerfile'
                }
            }
        }
        
        stage('Adding new line to a file') {
    steps {
        script {
            def fileName = 'next.config.js'

            // Read the file content
            def fileContent = readFile(fileName)

            // Define the new line to add and the target block to insert before its closing brace
            def newLine = "  output: 'standalone',"
            def targetBlockStart = "const nextConfig = {"
            def targetBlockEnd = "};"

            // Check if the new line already exists in the file
            def lineExists = fileContent.contains(newLine)

            if (lineExists) {
                echo "The line '${newLine.trim()}' already exists in ${fileName}. Skipping line addition."
            } else {
                // If the line doesn't exist, proceed with the logic to add it

                // Find the start and end of the nextConfig block
                int startIdx = fileContent.indexOf(targetBlockStart)
                int endIdx = fileContent.indexOf(targetBlockEnd, startIdx) + targetBlockEnd.length()

                // If the block is found, insert the new line before the closing brace
                if (startIdx != -1 && endIdx != -1) {
                    String before = fileContent.substring(0, endIdx - 2) // Up to before the closing brace of nextConfig
                    String after = fileContent.substring(endIdx - 2) // From the closing brace of nextConfig to the end
                    def modifiedContent = before + newLine + "\n" + after

                    // Write the modified content back to the file
                    writeFile file: fileName, text: modifiedContent

                    echo 'File updated successfully'
                } else {
                    echo 'nextConfig block not found'
                }
            }

            sh 'cat next.config.js'
                }
            }
        }
    }
}
}
