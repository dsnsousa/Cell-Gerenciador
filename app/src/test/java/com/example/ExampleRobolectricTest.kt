package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.database.DatabaseInitializer
import com.example.data.model.*
import com.example.data.repository.EcellRepository
import com.example.util.Formatters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: EcellRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = EcellRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context matches ECELL GESTAO`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("ECELL GESTÃO", appName)
    }

    @Test
    fun `test initial data seed and authentication for three profiles`() = runBlocking {
        DatabaseInitializer.populateInitialDataIfEmpty(db)

        val admin = repository.authenticate("admin", "admin123")
        assertNotNull(admin)
        assertEquals(UserRole.ADMIN, admin?.role)

        val tecnico = repository.authenticate("tecnico", "tec123")
        assertNotNull(tecnico)
        assertEquals(UserRole.TECNICO, tecnico?.role)

        val vendedor = repository.authenticate("vendedor", "ven123")
        assertNotNull(vendedor)
        assertEquals(UserRole.VENDEDOR, vendedor?.role)

        // Invalid password test
        val wrong = repository.authenticate("admin", "senhaerrada")
        assertNull(wrong)
    }

    @Test
    fun `test sale creation, automatic stock deduction and profit calculations`() = runBlocking {
        DatabaseInitializer.populateInitialDataIfEmpty(db)
        val seller = repository.authenticate("vendedor", "ven123")!!
        val customer = repository.getAllCustomers().first().first()

        val product = repository.getAllProducts().first().first { it.stockQuantity >= 2 }
        val initialStock = product.stockQuantity

        val cart = listOf(EcellRepository.CartItem(product, 2))
        val result = repository.confirmSale(
            customer = customer,
            seller = seller,
            items = cart,
            discount = 0.0,
            paymentMethod = PaymentMethod.PIX
        )

        assertTrue(result.isSuccess)
        val sale = result.getOrThrow()
        assertEquals(product.salePrice * 2, sale.total, 0.01)

        // Verify stock was decremented
        val freshProduct = repository.getProductById(product.id)!!
        assertEquals(initialStock - 2, freshProduct.stockQuantity)

        // Verify margin and profit helpers
        val unitProfit = Formatters.calculateUnitProfit(product.costPrice, product.salePrice)
        assertEquals(product.salePrice - product.costPrice, unitProfit, 0.01)

        val margin = Formatters.calculateMargin(60.0, 150.0)
        assertEquals(60.0, margin, 0.01)
    }

    @Test
    fun `test sale cancellation restores stock only for admin`() = runBlocking {
        DatabaseInitializer.populateInitialDataIfEmpty(db)
        val admin = repository.authenticate("admin", "admin123")!!
        val seller = repository.authenticate("vendedor", "ven123")!!

        val product = repository.getAllProducts().first().first { it.stockQuantity >= 2 }
        val initialStock = product.stockQuantity

        val cart = listOf(EcellRepository.CartItem(product, 1))
        val sale = repository.confirmSale(null, seller, cart, 0.0, PaymentMethod.DINHEIRO).getOrThrow()

        val stockAfterSale = repository.getProductById(product.id)!!.stockQuantity
        assertEquals(initialStock - 1, stockAfterSale)

        // Cancel sale
        val cancelResult = repository.cancelSale(sale.id, "Cliente desistiu", admin)
        assertTrue(cancelResult.isSuccess)

        // Stock restored
        val restoredStock = repository.getProductById(product.id)!!.stockQuantity
        assertEquals(initialStock, restoredStock)
    }
}
