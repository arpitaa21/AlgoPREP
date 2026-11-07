package com.placementportal.config;

import com.placementportal.entity.*;
import com.placementportal.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs once at start-up:
 *   1. makes sure the STUDENT and ADMIN roles exist
 *   2. creates the first admin account (nobody can register as admin through the API)
 *   3. optionally loads sample topics, questions, tests and company roadmaps so the app is not empty
 * Every step checks "does it already exist?" first, so restarting the app never duplicates data.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final TestSetRepository testRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    private final boolean seedAdmin;
    private final String adminEmail;
    private final String adminPassword;
    private final boolean seedSamples;

    public DataInitializer(RoleRepository roleRepository, UserRepository userRepository,
                           TopicRepository topicRepository, QuestionRepository questionRepository,
                           TestSetRepository testRepository, CompanyRepository companyRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.seed.admin-enabled}") boolean seedAdmin,
                           @Value("${app.seed.admin-email}") String adminEmail,
                           @Value("${app.seed.admin-password}") String adminPassword,
                           @Value("${app.seed.sample-data}") boolean seedSamples) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.testRepository = testRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedAdmin = seedAdmin;
        this.adminEmail = adminEmail.trim().toLowerCase();
        this.adminPassword = adminPassword;
        this.seedSamples = seedSamples;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Role studentRole = ensureRole(RoleName.STUDENT);
        Role adminRole = ensureRole(RoleName.ADMIN);

        if (seedAdmin && !userRepository.existsByEmail(adminEmail)) {
            User admin = new User("Portal Admin", adminEmail, passwordEncoder.encode(adminPassword));
            admin.getRoles().add(adminRole);
            admin.getRoles().add(studentRole);
            userRepository.save(admin);
        }
        if (seedSamples && topicRepository.count() == 0) {
            seedSampleData();
        }
    }

    private Role ensureRole(RoleName name) {
        return roleRepository.findByName(name).orElseGet(() -> roleRepository.save(new Role(name)));
    }

    // =====================================================================
    //  SAMPLE DATA
    // =====================================================================
    private void seedSampleData() {
        // ---- DSA topics ----
        Topic arrays = topic("Arrays", "Array and hashing problems");
        Topic strings = topic("Strings", "String manipulation and sliding window");
        Topic linked = topic("Linked List", "Pointer manipulation on lists");
        Topic stacks = topic("Stacks & Queues", "LIFO / FIFO problems");
        Topic trees = topic("Trees", "Binary trees and traversals");
        Topic graphs = topic("Graphs", "BFS, DFS and graph modelling");
        Topic dp = topic("Dynamic Programming", "Overlapping sub-problems and memoization");
        Topic search = topic("Searching & Sorting", "Binary search and sorting");
        // ---- MCQ topics ----
        Topic quant = topic("Quantitative Aptitude", "Percentages, ratios, speed, interest");
        Topic logic = topic("Logical Reasoning", "Series, coding-decoding, relations");
        Topic dbms = topic("DBMS", "SQL, normalization, transactions");
        Topic os = topic("Operating Systems", "Scheduling, deadlocks, memory");
        Topic oop = topic("OOP & Java", "Core object-oriented concepts");

        // ---- DSA questions (the problems live on LeetCode; we store the link + our approach notes) ----
        dsa(arrays, "Two Sum", Difficulty.EASY,
                "Given an array of integers and a target, return the indices of the two numbers that add up to the target.",
                "https://leetcode.com/problems/two-sum/",
                "Use a HashMap from value to index. For each number look up (target - number). O(n) time, O(n) space.");
        dsa(arrays, "Best Time to Buy and Sell Stock", Difficulty.EASY,
                "Given daily prices, find the maximum profit from one buy followed by one later sell.",
                "https://leetcode.com/problems/best-time-to-buy-and-sell-stock/",
                "Track the minimum price so far and the best (price - minimum). One pass, O(n).");
        dsa(arrays, "Maximum Subarray", Difficulty.MEDIUM,
                "Find the contiguous subarray with the largest sum.",
                "https://leetcode.com/problems/maximum-subarray/",
                "Kadane's algorithm: current = max(x, current + x); best = max(best, current).");
        dsa(arrays, "Product of Array Except Self", Difficulty.MEDIUM,
                "Return an array where each element is the product of all other elements, without using division.",
                "https://leetcode.com/problems/product-of-array-except-self/",
                "Prefix products on a left-to-right pass, then multiply by suffix products on a right-to-left pass.");
        dsa(strings, "Valid Anagram", Difficulty.EASY,
                "Decide whether two strings contain exactly the same characters with the same counts.",
                "https://leetcode.com/problems/valid-anagram/",
                "Count characters in an int[26] (or HashMap); the counts must cancel out.");
        dsa(strings, "Longest Substring Without Repeating Characters", Difficulty.MEDIUM,
                "Find the length of the longest substring that has no repeated character.",
                "https://leetcode.com/problems/longest-substring-without-repeating-characters/",
                "Sliding window with a set/map of last-seen indices; move the left edge when a repeat appears.");
        dsa(linked, "Reverse Linked List", Difficulty.EASY,
                "Reverse a singly linked list and return the new head.",
                "https://leetcode.com/problems/reverse-linked-list/",
                "Iterate with prev/current/next pointers, flipping each link. O(n) time, O(1) space.");
        dsa(linked, "Linked List Cycle", Difficulty.EASY,
                "Detect whether a linked list contains a cycle.",
                "https://leetcode.com/problems/linked-list-cycle/",
                "Floyd's tortoise and hare: a slow and a fast pointer meet if and only if there is a cycle.");
        dsa(linked, "Merge Two Sorted Lists", Difficulty.EASY,
                "Merge two sorted linked lists into one sorted list.",
                "https://leetcode.com/problems/merge-two-sorted-lists/",
                "Use a dummy head and repeatedly append the smaller node.");
        dsa(stacks, "Valid Parentheses", Difficulty.EASY,
                "Check that brackets (), [] and {} are closed in the correct order.",
                "https://leetcode.com/problems/valid-parentheses/",
                "Push opening brackets on a stack; on a closing bracket the top must be its match.");
        dsa(stacks, "Min Stack", Difficulty.MEDIUM,
                "Design a stack that supports push, pop, top and retrieving the minimum in O(1).",
                "https://leetcode.com/problems/min-stack/",
                "Keep a second stack that stores the running minimum for every depth.");
        dsa(trees, "Maximum Depth of Binary Tree", Difficulty.EASY,
                "Return the number of nodes on the longest root-to-leaf path.",
                "https://leetcode.com/problems/maximum-depth-of-binary-tree/",
                "Recursion: 1 + max(depth(left), depth(right)).");
        dsa(trees, "Binary Tree Level Order Traversal", Difficulty.MEDIUM,
                "Return the node values level by level.",
                "https://leetcode.com/problems/binary-tree-level-order-traversal/",
                "BFS with a queue; process the queue one level (its current size) at a time.");
        dsa(graphs, "Number of Islands", Difficulty.MEDIUM,
                "Count connected groups of land cells in a grid of land and water.",
                "https://leetcode.com/problems/number-of-islands/",
                "DFS/BFS from every unvisited land cell, marking the whole island as visited.");
        dsa(graphs, "Course Schedule", Difficulty.MEDIUM,
                "Given course prerequisites, decide whether all courses can be finished.",
                "https://leetcode.com/problems/course-schedule/",
                "Detect a cycle in a directed graph: topological sort (Kahn's algorithm) or DFS with colours.");
        dsa(dp, "Climbing Stairs", Difficulty.EASY,
                "Count the ways to climb n stairs taking 1 or 2 steps at a time.",
                "https://leetcode.com/problems/climbing-stairs/",
                "ways(n) = ways(n-1) + ways(n-2): a Fibonacci recurrence; keep only the last two values.");
        dsa(dp, "Coin Change", Difficulty.MEDIUM,
                "Find the fewest coins needed to make a given amount.",
                "https://leetcode.com/problems/coin-change/",
                "dp[a] = min over coins c of dp[a - c] + 1, bottom-up from 0 to the amount.");
        dsa(dp, "Longest Increasing Subsequence", Difficulty.HARD,
                "Find the length of the longest strictly increasing subsequence.",
                "https://leetcode.com/problems/longest-increasing-subsequence/",
                "O(n^2) DP first; then improve to O(n log n) with patience sorting and binary search.");
        dsa(search, "Binary Search", Difficulty.EASY,
                "Search a sorted array for a target in O(log n).",
                "https://leetcode.com/problems/binary-search/",
                "Maintain low/high; compute mid as low + (high - low) / 2 to avoid integer overflow.");
        dsa(search, "Search in Rotated Sorted Array", Difficulty.HARD,
                "Search for a target in a sorted array that was rotated at an unknown pivot.",
                "https://leetcode.com/problems/search-in-rotated-sorted-array/",
                "Binary search: at each step one half is still sorted; decide which half can contain the target.");

        // ---- MCQs: used by the timed tests and by targeted practice ----
        List<Question> aptitude = new ArrayList<>();
        aptitude.add(mcq(quant, "What is 15% of 240?", Difficulty.EASY, "0.15 x 240 = 36.", 2, "24", "30", "36", "42"));
        aptitude.add(mcq(quant, "A 150 m long train runs at 54 km/h. How long does it take to pass a pole?", Difficulty.MEDIUM,
                "54 km/h = 15 m/s, so time = 150 / 15 = 10 s.", 1, "8 s", "10 s", "12 s", "15 s"));
        aptitude.add(mcq(quant, "The ratio A : B is 3 : 5 and A = 18. What is B?", Difficulty.EASY,
                "One part = 18 / 3 = 6, so B = 5 x 6 = 30.", 1, "24", "30", "36", "45"));
        aptitude.add(mcq(quant, "Simple interest on Rs. 2000 at 5% per annum for 3 years is:", Difficulty.EASY,
                "SI = P x R x T / 100 = 2000 x 5 x 3 / 100 = 300.", 2, "200", "250", "300", "350"));
        aptitude.add(mcq(logic, "Find the next number: 2, 6, 12, 20, 30, ?", Difficulty.MEDIUM,
                "Differences are 4, 6, 8, 10, so the next difference is 12: 30 + 12 = 42.", 1, "40", "42", "44", "36"));
        aptitude.add(mcq(logic, "All roses are flowers. Some flowers fade quickly. Which statement is definitely true?", Difficulty.EASY,
                "Only the first premise is certain; nothing definite follows about roses fading.", 2,
                "All roses fade quickly", "Some roses fade quickly", "All roses are flowers", "No rose fades quickly"));
        aptitude.add(mcq(logic, "If CAT is written as DBU, how is DOG written?", Difficulty.EASY,
                "Each letter moves one step forward: D->E, O->P, G->H.", 0, "EPH", "DPH", "EOH", "FPI"));
        // extra aptitude questions that are NOT in a test (used for targeted practice)
        mcq(quant, "What is the average of the first five positive integers?", Difficulty.EASY,
                "(1+2+3+4+5) / 5 = 3.", 1, "2", "3", "3.5", "4");
        mcq(quant, "A can finish a job in 12 days and B in 24 days. Working together they finish it in:", Difficulty.MEDIUM,
                "Combined rate = 1/12 + 1/24 = 3/24 = 1/8 per day, so 8 days.", 1, "6 days", "8 days", "9 days", "10 days");
        mcq(logic, "Pointing to a man, Riya says: 'He is the son of my mother's only brother.' The man is Riya's:", Difficulty.MEDIUM,
                "Her mother's brother is her maternal uncle; his son is her cousin.", 1, "Brother", "Cousin", "Uncle", "Nephew");

        List<Question> cs = new ArrayList<>();
        cs.add(mcq(dbms, "Which normal form removes partial dependency on a composite key?", Difficulty.MEDIUM,
                "2NF requires every non-key attribute to depend on the WHOLE key.", 1, "1NF", "2NF", "3NF", "BCNF"));
        cs.add(mcq(dbms, "Which SQL clause filters groups after GROUP BY?", Difficulty.EASY,
                "WHERE filters rows before grouping; HAVING filters the groups.", 1, "WHERE", "HAVING", "ORDER BY", "LIMIT"));
        cs.add(mcq(dbms, "In ACID, the letter D stands for:", Difficulty.EASY,
                "Durability: once committed, data survives crashes.", 1, "Dependency", "Durability", "Distribution", "Deadlock"));
        cs.add(mcq(dbms, "Which constraint guarantees a column value uniquely identifies a row and is never NULL?", Difficulty.EASY,
                "A PRIMARY KEY is unique and NOT NULL.", 1, "FOREIGN KEY", "PRIMARY KEY", "CHECK", "DEFAULT"));
        cs.add(mcq(os, "Which scheduling algorithm can starve long processes?", Difficulty.MEDIUM,
                "Shortest Job First keeps picking short jobs, so a long job may wait forever.", 2,
                "First Come First Serve", "Round Robin", "Shortest Job First", "None of these"));
        cs.add(mcq(os, "Which of these is NOT one of the four necessary conditions for deadlock?", Difficulty.MEDIUM,
                "The condition is NO preemption; being able to preempt resources prevents deadlock.", 2,
                "Mutual exclusion", "Hold and wait", "Preemption of resources", "Circular wait"));
        cs.add(mcq(os, "Which memory-management technique divides memory into fixed-size blocks?", Difficulty.EASY,
                "Paging uses fixed-size pages and frames; segmentation uses variable-size segments.", 1,
                "Segmentation", "Paging", "Swapping", "Overlays"));
        cs.add(mcq(oop, "Making fields private and exposing them through getters/setters is an example of:", Difficulty.EASY,
                "That is encapsulation: bundling data with controlled access.", 1,
                "Inheritance", "Encapsulation", "Overloading", "Recursion"));
        cs.add(mcq(oop, "Which Java keyword prevents a method from being overridden?", Difficulty.EASY,
                "A final method cannot be overridden by subclasses.", 1,
                "abstract", "final", "synchronized", "transient"));
        cs.add(mcq(oop, "Method overloading is resolved at:", Difficulty.MEDIUM,
                "Overloads are chosen by the compiler from the argument types (static binding).", 0,
                "Compile time", "Run time", "Load time", "Link time"));
        // extra DBMS practice questions (not in a test)
        mcq(dbms, "Which JOIN returns all rows of the left table, even when there is no match on the right?", Difficulty.EASY,
                "LEFT JOIN keeps every left row and fills missing right columns with NULL.", 1,
                "INNER JOIN", "LEFT JOIN", "CROSS JOIN", "SELF JOIN");
        mcq(dbms, "What does a database index primarily improve?", Difficulty.EASY,
                "Indexes speed up lookups at the cost of extra storage and slower writes.", 0,
                "Speed of data retrieval", "Storage capacity", "Password security", "Table width");

        // ---- Timed tests ----
        test("Aptitude Basics", "Quantitative and logical reasoning warm-up", 10, aptitude);
        test("CS Fundamentals", "DBMS, Operating Systems and OOP basics", 15, cs);

        // ---- Company roadmaps ----
        Company tcs = company("TCS", "Typical preparation path for large service-company recruitment tests (e.g. TCS NQT style).");
        step(tcs, 1, "Aptitude foundation", "Practise percentages, ratios, time-speed-distance and interest daily.", quant, null);
        step(tcs, 2, "Logical reasoning", "Series, coding-decoding and blood relations.", logic, null);
        step(tcs, 3, "Programming basics", "Arrays and strings problems in your strongest language.", arrays, null);
        step(tcs, 4, "CS fundamentals", "Revise DBMS and OOP for the technical interview.", dbms, null);

        Company infosys = company("Infosys", "Typical path for Infosys-style online assessment and interview.");
        step(infosys, 1, "Aptitude and reasoning", "Timed sets of quantitative and logical questions.", quant, null);
        step(infosys, 2, "Core programming", "Strings, arrays and basic recursion.", strings, null);
        step(infosys, 3, "Database and SQL", "Joins, GROUP BY / HAVING, normalization.", dbms, null);
        step(infosys, 4, "OOP concepts", "Encapsulation, inheritance, polymorphism with Java examples.", oop, null);

        Company amazon = company("Amazon", "Typical path for product-company online assessments that stress data structures and algorithms.");
        step(amazon, 1, "Arrays and hashing", "Master two-pointer, prefix-sum and hash-map patterns.", arrays, "https://leetcode.com/problemset/");
        step(amazon, 2, "Linked lists and stacks", "Pointer manipulation and monotonic stack ideas.", linked, null);
        step(amazon, 3, "Trees and graphs", "BFS/DFS on trees, grids and graphs.", graphs, null);
        step(amazon, 4, "Dynamic programming", "Start with 1-D DP, then 2-D DP.", dp, null);
        step(amazon, 5, "Mock assessments", "Take timed tests and review mistakes by topic.", search, null);
    }

    // ---------------------------------------------------------------------
    //  small helpers to keep the seed readable
    // ---------------------------------------------------------------------
    private Topic topic(String name, String description) {
        return topicRepository.save(new Topic(name, description));
    }

    private Question dsa(Topic topic, String title, Difficulty difficulty, String description, String link, String explanation) {
        Question q = Question.create(title, description, difficulty, QuestionType.DSA, topic);
        q.setExternalLink(link);
        q.setExplanation(explanation);
        return questionRepository.save(q);
    }

    /** correctIndex = position (0-based) of the right option inside the options varargs. */
    private Question mcq(Topic topic, String title, Difficulty difficulty, String explanation,
                         int correctIndex, String... options) {
        Question q = Question.create(title, "Choose the correct option.", difficulty, QuestionType.MCQ, topic);
        q.setExplanation(explanation);
        for (int i = 0; i < options.length; i++) {
            q.addOption(new QuestionOption(options[i], i == correctIndex));
        }
        return questionRepository.save(q);
    }

    private void test(String title, String description, int minutes, List<Question> questions) {
        TestSet t = new TestSet(title, description, minutes, true);
        t.getQuestions().addAll(questions);
        testRepository.save(t);
    }

    private Company company(String name, String description) {
        return companyRepository.save(new Company(name, description));
    }

    private void step(Company company, int order, String title, String description, Topic topic, String url) {
        company.getRoadmap().add(new CompanyRoadmap(company, order, title, description, topic, url));
    }
}
