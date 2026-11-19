<h1>Costco Checkout Simulator (Producer-Consumer Lab)</h1>
<p><strong>Classes</strong></p>
<ul>
<li><strong>Customer (Producer)</strong> - shops for a random time, then joins the queue.</li>
<li><strong>Cashier (Consumer)</strong> - takes customer IDs from the queue and serves them; stops on a poison pill (-1).</li>
<li><strong>CheckoutQueue</strong> - wraps ArrayBlockingQueue<Integer>; put blocks when full, take blocks when empty.</li>
<li><strong>CostcoSimulation</strong> - prompts for customers / cashiers / queue size; --batch runs all experiments.</li>
</ul>

<h2>Factory simulation</h2>
<p>The JavaFX factory simulation shows separate incoming-parts and finished-product conveyors, with parts moving toward assemblers and packagers respectively. Run it from the repository root with <code>mvn javafx:run</code>. In IntelliJ, run <code>factory.FactoryLauncher</code> (not <code>factory.FactoryApp</code>) so JavaFX starts correctly from the classpath.</p>

<h2>Experiment results</h2>

| Setup | Configuration | Avg Wait | Max Wait | Utilization | Cost |
|---|---|---:|---:|---:|---:|
| 1 | 1 cashier, 20 customers | 14.4 min | 28.6 min | 94.2% | $14.95 |
| 2 | 3 cashiers, 60 customers | 0.5 min | 3.0 min | 78.6% | $56.04 |
| 3 | 10 cashiers, 60 customers | 0.0 min | 0.2 min | 24.1% | $187.71 |
| 4 | 3 cashiers, queue = 2 | 1.1 min | 4.5 min | 78.9% | $56.23 |
| 5 | 3 cashiers, queue = 60 | 0.9 min | 4.7 min | 79.7% | $55.66 |